/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.session.internal;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.http.Response;
import ratpack.session.*;
import ratpack.util.Exceptions;
import ratpack.util.Types;

import java.io.*;
import java.util.*;

public class DefaultSession implements Session {

  private final Map<SessionKey<?>, byte[]> entries = Maps.newHashMap();

  private final SessionId sessionId;
  private final ByteBufAllocator bufferAllocator;
  private final SessionStore storeAdapter;
  private final Response response;
  private final SessionSerializer defaultSerializer;
  private final JavaSessionSerializer javaSerializer;

  private enum State {
    NOT_LOADED, CLEAN, DIRTY
  }

  private State state = State.NOT_LOADED;
  private boolean callbackAdded;

  private final SessionData data = new Data();

  private static class SerializedForm implements Serializable {
    private static final long serialVersionUID = 1;
    Map<SessionKey<?>, byte[]> entries;

    public SerializedForm(Map<SessionKey<?>, byte[]> entries) {
      this.entries = entries;
    }
  }

  public DefaultSession(SessionId sessionId, ByteBufAllocator bufferAllocator, SessionStore storeAdapter, Response response, SessionSerializer defaultSerializer, JavaSessionSerializer javaSerializer) {
    this.sessionId = sessionId;
    this.bufferAllocator = bufferAllocator;
    this.storeAdapter = storeAdapter;
    this.response = response;
    this.defaultSerializer = defaultSerializer;
    this.javaSerializer = javaSerializer;
  }

  @Override
  public String getId() {
    return sessionId.getValue().toString();
  }

  @Override
  public Promise<SessionData> getData() {
    if (state == State.NOT_LOADED) {
      return storeAdapter.load(sessionId.getValue()).map(bytes -> {
        state = State.CLEAN;
        try {
          hydrate(bytes);
        } finally {
          bytes.release();
        }
        return data;
      });
    } else {
      return Promise.value(data);
    }
  }

  private void hydrate(ByteBuf bytes) {
    if (bytes.readableBytes() > 0) {
      try {
        SerializedForm deserialized = defaultSerializer.deserialize(SerializedForm.class, new ByteBufInputStream(bytes));
        entries.clear();
        entries.putAll(deserialized.entries);
      } catch (Exception e) {
        throw Exceptions.uncheck(e);
      }
    }
  }

  @Override
  public JavaSessionSerializer getJavaSerializer() {
    return javaSerializer;
  }

  @Override
  public SessionSerializer getDefaultSerializer() {
    return defaultSerializer;
  }

  @Override
  public boolean isDirty() {
    return state == State.DIRTY;
  }

  private ByteBuf serialize() throws Exception {
    SerializedForm serializable = new SerializedForm(ImmutableMap.copyOf(this.entries));
    ByteBuf buffer = bufferAllocator.buffer();
    OutputStream outputStream = new ByteBufOutputStream(buffer);
    try {
      defaultSerializer.serialize(SerializedForm.class, serializable, outputStream);
      outputStream.close();
      return buffer;
    } catch (Throwable e) {
      buffer.release();
      throw e;
    }
  }

  @Override
  public Operation save() {
    return Operation.of(() -> {
      if (state != State.NOT_LOADED) {
        ByteBuf serialized = serialize();
        storeAdapter.store(sessionId.getValue(), serialized)
          .wiretap(o -> serialized.release())
          .next(() -> state = State.CLEAN)
          .then();
      }
    });
  }

  @Override
  public Operation terminate() {
    return storeAdapter.remove(sessionId.getValue())
      .next(() -> {
        sessionId.terminate();
        entries.clear();
        state = State.NOT_LOADED;
      });
  }

  private void markDirty() {
    state = State.DIRTY;
    if (!callbackAdded) {
      callbackAdded = true;
      response.beforeSend(responseMetaData -> {
        callbackAdded = false; // another before send may try and use the session
        if (state == State.DIRTY) {
          save().then();
        }
      });
    }
  }

  private class Data implements SessionData {

    @Override
    public <T> Optional<T> get(SessionKey<T> key, SessionSerializer serializer) {
      String name = key.getName();
      if (key.getType() == null) {
        key = Types.cast(findKey(name));
        if (key == null) {
          return Optional.empty();
        }
      }

      byte[] bytes = entries.get(key);
      if (bytes == null) {
        return Optional.empty();
      } else {
        try {
          T deserialized = serializer.deserialize(key.getType(), new ByteArrayInputStream(bytes));
          return Optional.of(deserialized);
        } catch (IOException e) {
          throw Exceptions.uncheck(e);
        }
      }
    }

    private SessionKey<?> findKey(String name) {
      List<Map.Entry<SessionKey<?>, byte[]>> entries = FluentIterable.from(DefaultSession.this.entries.entrySet())
        .filter(e -> Objects.equals(e.getKey().getName(), name))
        .toList();

      if (entries.isEmpty()) {
        return null;
      } else if (entries.size() == 1) {
        return entries.get(0).getKey();
      } else {
        throw new IllegalArgumentException("Found more than one session entry with name '" + name + "': " + Iterables.transform(entries, Map.Entry::getKey));
      }
    }

    @Override
    public <T> void set(SessionKey<T> key, T value, SessionSerializer serializer) {
      Objects.requireNonNull(key, "session key cannot be null");
      Objects.requireNonNull(value, "session value for key " + key.getName() + " cannot be null");

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try {
        serializer.serialize(key.getType(), value, out);
      } catch (IOException e) {
        throw Exceptions.uncheck(e);
      }

      entries.put(key, out.toByteArray());
      markDirty();
    }

    @Override
    public Set<SessionKey<?>> getKeys() {
      return entries.keySet();
    }

    @Override
    public SessionSerializer getDefaultSerializer() {
      return defaultSerializer;
    }

    @Override
    public void remove(SessionKey<?> key) {
      if (key.getType() == null) {
        key = findKey(key.getName());
        if (key == null) {
          return;
        }
      }
      entries.remove(key);
      markDirty();
    }

    @Override
    public void clear() {
      entries.clear();
      markDirty();
    }

    @Override
    public Session getSession() {
      return DefaultSession.this;
    }
  }
}
