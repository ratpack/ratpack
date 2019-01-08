/*
 * Copyright 2013 the original author or authors.
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

package ratpack.http.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;
import org.reactivestreams.Publisher;
import ratpack.api.Nullable;
import ratpack.exec.Operation;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.func.Action;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.util.MultiValueMap;

import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

import static ratpack.http.internal.HttpHeaderConstants.CONTENT_TYPE;

public class DefaultResponse implements Response {

  private Status status = Status.OK;
  private final MutableHeaders headers;
  private final ByteBufAllocator byteBufAllocator;
  private final ResponseTransmitter responseTransmitter;

  private boolean contentTypeSet;
  private Set<Cookie> cookies;
  private List<Action<? super Response>> responseFinalizers;

  public DefaultResponse(MutableHeaders headers, ByteBufAllocator byteBufAllocator, ResponseTransmitter responseTransmitter) {
    this.byteBufAllocator = byteBufAllocator;
    this.responseTransmitter = responseTransmitter;
    this.headers = new MutableHeadersWrapper(headers);
    this.responseFinalizers = Lists.newArrayList();
  }

  class MutableHeadersWrapper implements MutableHeaders {

    private final MutableHeaders wrapped;

    MutableHeadersWrapper(MutableHeaders wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public MutableHeaders add(CharSequence name, Object value) {
      if (!contentTypeSet && (name == CONTENT_TYPE || name.toString().equalsIgnoreCase(CONTENT_TYPE.toString()))) {
        contentTypeSet = true;
      }

      wrapped.add(name, value);
      return this;
    }

    @Override
    public MutableHeaders set(CharSequence name, Object value) {
      if (!contentTypeSet && (name == CONTENT_TYPE || name.toString().equalsIgnoreCase(CONTENT_TYPE.toString()))) {
        contentTypeSet = true;
      }

      wrapped.set(name, value);
      return this;
    }

    @Override
    public MutableHeaders setDate(CharSequence name, Date value) {
      wrapped.set(name, value);
      return this;
    }

    @Override
    public MutableHeaders set(CharSequence name, Iterable<?> values) {
      if (!contentTypeSet && (name == CONTENT_TYPE || name.toString().equalsIgnoreCase(CONTENT_TYPE.toString()))) {
        contentTypeSet = true;
      }

      wrapped.set(name, values);
      return this;
    }

    @Override
    public MutableHeaders remove(CharSequence name) {
      if (name == CONTENT_TYPE || name.toString().equalsIgnoreCase(CONTENT_TYPE.toString())) {
        contentTypeSet = false;
      }

      wrapped.remove(name);
      return this;
    }

    @Override
    public MutableHeaders clear() {
      contentTypeSet = false;
      wrapped.clear();
      return this;
    }

    @Override
    public MutableHeaders copy(Headers headers) {
      this.wrapped.copy(headers);
      if (headers.contains(HttpHeaderConstants.CONTENT_TYPE)) {
        contentTypeSet = true;
      }
      return this;
    }

    @Override
    public MultiValueMap<String, String> asMultiValueMap() {
      return wrapped.asMultiValueMap();
    }

    @Nullable
    @Override
    public String get(CharSequence name) {
      return wrapped.get(name);
    }

    @Nullable
    @Override
    public String get(String name) {
      return wrapped.get(name);
    }

    @Nullable
    @Override
    public Date getDate(CharSequence name) {
      return wrapped.getDate(name);
    }

    @Override
    @Nullable
    public Date getDate(String name) {
      return wrapped.getDate(name);
    }

    @Override
    public List<String> getAll(CharSequence name) {
      return wrapped.getAll(name);
    }

    @Override
    public List<String> getAll(String name) {
      return wrapped.getAll(name);
    }

    @Override
    public boolean contains(CharSequence name) {
      return wrapped.contains(name);
    }

    @Override
    public boolean contains(String name) {
      return wrapped.contains(name);
    }

    @Override
    public Set<String> getNames() {
      return wrapped.getNames();
    }

    @Override
    public HttpHeaders getNettyHeaders() {
      return wrapped.getNettyHeaders();
    }
  }

  public Status getStatus() {
    return status;
  }

  @Override
  public Response status(Status status) {
    this.status = status;
    return this;
  }

  @Override
  public Response noCompress() {
    headers.set(HttpHeaderNames.CONTENT_ENCODING, HttpHeaderValues.IDENTITY);
    return this;
  }

  @Override
  public MutableHeaders getHeaders() {
    return headers;
  }

  public void send() {
    commit(Unpooled.EMPTY_BUFFER);
  }

  @Override
  public Response contentTypeIfNotSet(Supplier<CharSequence> contentType) {
    if (!contentTypeSet) {
      contentType(contentType.get());
    }
    return this;
  }

  @Override
  public Response contentType(CharSequence contentType) {
    headers.set(CONTENT_TYPE, contentType);
    return this;
  }

  @Override
  public Response contentTypeIfNotSet(CharSequence contentType) {
    if (!contentTypeSet) {
      contentType(contentType);
    }
    return this;
  }

  public void send(String text) {
    ByteBuf byteBuf = ByteBufUtil.encodeString(byteBufAllocator, CharBuffer.wrap(text), CharsetUtil.UTF_8);
    contentTypeIfNotSet(HttpHeaderConstants.PLAIN_TEXT_UTF8).send(byteBuf);
  }

  public void send(CharSequence contentType, String body) {
    contentType(contentType);
    send(body);
  }

  public void send(byte[] bytes) {
    contentTypeIfNotSet(HttpHeaderConstants.OCTET_STREAM);
    commit(Unpooled.wrappedBuffer(bytes));
  }

  public void send(CharSequence contentType, byte[] bytes) {
    contentType(contentType).send(bytes);
  }

  public void send(CharSequence contentType, ByteBuf buffer) {
    contentType(contentType);
    send(buffer);
  }

  public void send(ByteBuf buffer) {
    contentTypeIfNotSet(HttpHeaderConstants.OCTET_STREAM);
    commit(buffer);
  }

  public void sendFile(Path file) {
    finalizeResponse(() -> {
      setCookieHeader();
      responseTransmitter.transmit(status.getNettyStatus(), file);
    });
  }

  @Override
  public void sendStream(Publisher<? extends ByteBuf> stream) {
    finalizeResponse(() -> {
      setCookieHeader();
      stream.subscribe(responseTransmitter.transmitter(status.getNettyStatus()));
    });
  }

  @Override
  public Response beforeSend(Action<? super Response> responseFinalizer) {
    responseFinalizers.add(responseFinalizer);
    return this;
  }

  private static class OverwritingSet<T> extends HashSet<T> {
    @Override
    public boolean add(T item) {
      if (!super.add(item)) {
        super.remove(item);
        return super.add(item);
      } else {
        return true;
      }
    }
  }

  public Set<Cookie> getCookies() {
    if (cookies == null) {
      cookies = new OverwritingSet<>();
    }
    return cookies;
  }

  public Cookie cookie(String name, String value) {
    Cookie cookie = new DefaultCookie(name, value);
    getCookies().add(cookie);
    return cookie;
  }

  public Cookie expireCookie(String name) {
    Cookie cookie = cookie(name, "");
    cookie.setMaxAge(0);
    return cookie;
  }

  private void setCookieHeader() {
    if (cookies != null && !cookies.isEmpty()) {
      for (Cookie cookie : cookies) {
        headers.add(HttpHeaderConstants.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
      }
    }
  }

  private void commit(ByteBuf buffer) {
    int readableBytes = buffer.readableBytes();
    if (readableBytes > 0 || !mustNotHaveBody()) {
      headers.set(HttpHeaderNames.CONTENT_LENGTH, readableBytes);
    }
    finalizeResponse(() -> {
      setCookieHeader();
      responseTransmitter.transmit(status.getNettyStatus(), buffer);
    });
  }

  private boolean mustNotHaveBody() {
    int code = status.getCode();
    return (code >= 100 && code < 200) || code == 204 || code == 304;
  }

  private void finalizeResponse(Runnable then) {
    List<Action<? super Response>> finalizersCopy = ImmutableList.copyOf(responseFinalizers);
    responseFinalizers.clear();
    if (finalizersCopy.isEmpty()) {
      then.run();
    } else {
      finalizeResponse(finalizersCopy.iterator(), then);
    }
  }

  private void finalizeResponse(Iterator<Action<? super Response>> finalizers, Runnable then) {
    if (finalizers.hasNext()) {
      finalizers
        .next()
        .curry(this)
        .map(Operation::of)
        .then(() ->
          finalizeResponse(finalizers, then)
        );
    } else {
      finalizeResponse(then);
    }
  }

}
