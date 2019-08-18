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
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.http.Response;
import ratpack.session.*;
import ratpack.util.Types;

import java.io.*;
import java.util.*;

public class DefaultSession implements Session {

  private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);

  private Map<SessionKey<?>, byte[]> entries;

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

  private static class SerializedForm implements Externalizable {

    private static final long serialVersionUID = 2;

    private static final Ordering<SessionKey<?>> KEY_NAME_ORDERING = Ordering.natural()
      .nullsFirst()
      .onResultOf(SessionKey::getName);

    private static final Ordering<SessionKey<?>> KEY_TYPE_ORDERING = Ordering.natural()
      .nullsFirst()
      .onResultOf(k -> k.getType() == null ? null : k.getType().getName());

    private static final Comparator<SessionKey<?>> COMPARATOR = KEY_NAME_ORDERING.compound(KEY_TYPE_ORDERING);

    private Map<SessionKey<?>, byte[]> entries;

    public SerializedForm() {
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeShort(1); // schema version

      out.writeShort(entries.size());

      Map<SessionKey<?>, byte[]> sorted = ImmutableSortedMap.copyOf(entries, COMPARATOR);

      for (Map.Entry<SessionKey<?>, byte[]> entry : sorted.entrySet()) {
        String name = entry.getKey().getName();
        if (name == null) {
          out.writeBoolean(false);
        } else {
          out.writeBoolean(true);
          out.writeUTF(name);
        }

        Class<?> type = entry.getKey().getType();
        if (type == null) {
          out.writeBoolean(false);
        } else {
          out.writeBoolean(true);
          out.writeUTF(type.getName());
        }

        byte[] bytes = entry.getValue();
        out.writeInt(bytes.length);
        out.write(bytes);
      }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

      in.readShort(); // schema version

      short num = in.readShort();
      entries = new HashMap<>(num);
      for (short i = 0; i < num; ++i) {
        String name = in.readBoolean() ? in.readUTF() : null;

        Class<Object> type;
        if (in.readBoolean()) {
          String typeName = in.readUTF();
          Class<?> o = classLoader.loadClass(typeName);
          type = Types.cast(o);
        } else {
          type = null;
        }

        int bytesLength = in.readInt();
        byte[] bytes = new byte[bytesLength];
        int read = in.read(bytes);
        while (read < bytesLength) {
          read += in.read(bytes, read, bytesLength - read);
        }

        entries.put(new DefaultSessionKey<>(name, type), bytes);
      }
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

  private void hydrate(ByteBuf bytes) throws Exception {
    if (bytes.readableBytes() > 0) {
      try {
        SerializedForm deserialized = defaultSerializer.deserialize(SerializedForm.class, new ByteBufInputStream(bytes));
        if (deserialized == null) {
          this.entries = new HashMap<>();
        } else {
          entries = deserialized.entries;
        }
      } catch (Exception e) {
        LOGGER.warn("Exception thrown deserializing session " + getId() + " with serializer " + defaultSerializer + " (session will be discarded)", e);
        this.entries = new HashMap<>();
        markDirty();
      }
    } else {
      this.entries = new HashMap<>();
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
    SerializedForm serializable = new SerializedForm();
    serializable.entries = entries;
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
          .then(() -> state = State.CLEAN);
      }
    });
  }

  @Override
  public Operation terminate() {
    return storeAdapter.remove(sessionId.getValue())
      .next(() -> {
        sessionId.terminate();
        if (entries != null) {
          entries.clear();
        }
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
    public <T> Optional<T> get(SessionKey<T> key, SessionSerializer serializer) throws Exception {
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
          T value = serializer.deserialize(key.getType(), new ByteArrayInputStream(bytes));
          return Optional.ofNullable(value);
        } catch (Exception e) {
          LOGGER.warn("Exception thrown deserializing entry " + key + " with serializer " + serializer + " (value will be discarded from session)", e);
          remove(key);
          return Optional.empty();
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
    public <T> void set(SessionKey<T> key, T value, SessionSerializer serializer) throws Exception {
      Objects.requireNonNull(key, "session key cannot be null");
      Objects.requireNonNull(value, "session value for key " + key.getName() + " cannot be null");

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      serializer.serialize(key.getType(), value, out);

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
      if (entries.remove(key) != null) {
        markDirty();
      }
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
