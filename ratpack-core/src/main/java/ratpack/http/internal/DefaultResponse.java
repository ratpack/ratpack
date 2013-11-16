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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.ServerCookieEncoder;
import ratpack.background.Background;
import ratpack.file.internal.FileHttpTransmitter;
import ratpack.handling.ContextComplete;
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.util.Action;
import ratpack.util.internal.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ratpack.file.internal.DefaultFileRenderer.readAttributes;

public class DefaultResponse implements Response {

  private final Status status;
  private final MutableHeaders headers;
  private final ByteBuf body;
  private final FileHttpTransmitter fileHttpTransmitter;
  private final Runnable committer;
  private final ChannelPromise completePromise;

  private boolean contentTypeSet;
  private Set<Cookie> cookies;

  public DefaultResponse(Status status, MutableHeaders headers, ByteBuf body, FileHttpTransmitter fileHttpTransmitter, ChannelPromise completePromise, Runnable committer) {
    this.status = status;
    this.fileHttpTransmitter = fileHttpTransmitter;
    this.headers = new MutableHeadersWrapper(headers);
    this.body = body;
    this.committer = committer;
    this.completePromise = completePromise;
  }

  class MutableHeadersWrapper implements MutableHeaders {

    private final MutableHeaders wrapped;

    MutableHeadersWrapper(MutableHeaders wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public void add(String name, Object value) {
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
        contentTypeSet = true;
      }

      wrapped.add(name, value);
    }

    @Override
    public void set(String name, Object value) {
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
        contentTypeSet = true;
      }

      wrapped.set(name, value);
    }

    @Override
    public void setDate(String name, Date value) {
      wrapped.set(name, value);
    }

    @Override
    public void set(String name, Iterable<?> values) {
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
        contentTypeSet = true;
      }

      wrapped.set(name, values);
    }

    @Override
    public void remove(String name) {
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
        contentTypeSet = false;
      }

      wrapped.remove(name);
    }

    @Override
    public void clear() {
      contentTypeSet = false;
      wrapped.clear();
    }

    @Override
    public String get(String name) {
      return wrapped.get(name);
    }

    @Override
    public Date getDate(String name) {
      return wrapped.getDate(name);
    }

    @Override
    public List<String> getAll(String name) {
      return wrapped.getAll(name);
    }

    @Override
    public boolean contains(String name) {
      return wrapped.contains(name);
    }

    @Override
    public Set<String> getNames() {
      return wrapped.getNames();
    }
  }

  public Status getStatus() {
    return status;
  }

  public Response status(int code) {
    status.set(code);
    return this;
  }

  public Response status(int code, String message) {
    status.set(code, message);
    return this;
  }

  @Override
  public ByteBuf getBody() {
    return body;
  }

  @Override
  public MutableHeaders getHeaders() {
    return headers;
  }

  public void send() {
    commit();
  }

  @Override
  public Response contentType(String contentType) {
    headers.set(HttpHeaders.Names.CONTENT_TYPE, DefaultMediaType.utf8(contentType).toString());
    return this;
  }

  public void send(String text) {
    if (!contentTypeSet) {
      contentType("text/plain");
    }

    body.writeBytes(IoUtils.utf8Bytes(text));
    commit();
  }

  public void send(String contentType, String body) {
    contentType(contentType);
    send(body);
  }

  public void send(byte[] bytes) {
    if (!contentTypeSet) {
      contentType("application/octet-stream");
    }

    body.writeBytes(bytes);
    commit();
  }

  public void send(String contentType, byte[] bytes) {
    contentType(contentType).send(bytes);
  }

  @Override
  public void send(InputStream inputStream) throws IOException {
    IoUtils.writeTo(inputStream, body);
    commit();
  }

  @Override
  public void send(String contentType, InputStream inputStream) throws IOException {
    contentType(contentType).send(inputStream);
  }

  public void send(String contentType, ByteBuf buffer) {
    contentType(contentType);
    send(buffer);
  }

  public void send(ByteBuf buffer) {
    if (!contentTypeSet) {
      contentType("application/octet-stream");
    }

    body.writeBytes(buffer);
    commit();
  }

  @Override
  public void sendFile(Background background, String contentType, BasicFileAttributes attributes, File file) {
    contentType(contentType);
    setCookieHeader();
    fileHttpTransmitter.transmit(background, attributes, file);
  }

  @Override
  public void onComplete(final Action<ContextComplete> callback) {
    this.completePromise.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) {
        callback.execute(new ContextComplete());
      }
    });
  }

  public void sendFile(final Background background, final String contentType, final File file) {
    readAttributes(background, file, new Action<BasicFileAttributes>() {
      public void execute(BasicFileAttributes fileAttributes) {
        sendFile(background, contentType, fileAttributes, file);
      }
    });
  }

  public Set<Cookie> getCookies() {
    if (cookies == null) {
      cookies = new HashSet<>();
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
        headers.add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));
      }
    }
  }

  private void commit() {
    setCookieHeader();
    committer.run();
  }
}
