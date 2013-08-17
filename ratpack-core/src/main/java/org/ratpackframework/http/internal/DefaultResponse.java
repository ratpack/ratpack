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

package org.ratpackframework.http.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.ratpackframework.block.Blocking;
import org.ratpackframework.file.internal.FileHttpTransmitter;
import org.ratpackframework.http.MutableHeaders;
import org.ratpackframework.http.Response;
import org.ratpackframework.util.internal.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultResponse implements Response {

  private final MutableHeaders headers;
  private final FullHttpResponse response;
  private final FullHttpRequest request;
  private final Channel channel;
  private final ByteBufAllocator bufferAllocator;
  private final boolean keepAlive;
  private final HttpVersion version;
  private boolean contentLengthSet;
  private boolean contentTypeSet;

  private Set<Cookie> cookies;

  public DefaultResponse(MutableHeaders headers, FullHttpResponse response, FullHttpRequest request, Channel channel, ByteBufAllocator bufferAllocator, boolean keepAlive, HttpVersion version) {
    this.headers = new MutableHeadersWrapper(headers);
    this.response = response;
    this.request = request;
    this.channel = channel;
    this.bufferAllocator = bufferAllocator;
    this.keepAlive = keepAlive;
    this.version = version;
  }

  class MutableHeadersWrapper implements MutableHeaders {

    private final MutableHeaders wrapped;

    MutableHeadersWrapper(MutableHeaders wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public void add(String name, Object value) {
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
        contentLengthSet = true;
      }
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
        contentTypeSet = true;
      }

      wrapped.add(name, value);
    }

    @Override
    public void set(String name, Object value) {
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
        contentLengthSet = true;
      }
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
        contentTypeSet = true;
      }

      wrapped.set(name, value);
    }

    @Override
    public void set(String name, Iterable<?> values) {
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
        contentLengthSet = true;
      }
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
        contentTypeSet = true;
      }

      wrapped.set(name, values);
    }

    @Override
    public void remove(String name) {
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
        contentLengthSet = false;
      }
      if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
        contentTypeSet = false;
      }

      wrapped.remove(name);
    }

    @Override
    public void clear() {
      contentLengthSet = false;
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
    return new Status() {
      public int getCode() {
        return response.getStatus().code();
      }

      public String getMessage() {
        return response.getStatus().reasonPhrase();
      }
    };
  }

  public Response status(int code) {
    response.setStatus(HttpResponseStatus.valueOf(code));
    return this;
  }

  public Response status(int code, String message) {
    response.setStatus(new HttpResponseStatus(code, message));
    return this;
  }

  @Override
  public MutableHeaders getHeaders() {
    return headers;
  }

  public void send() {
    contentLengthSet = true;
    headers.set(HttpHeaders.Names.CONTENT_LENGTH, 0);
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

    send(IoUtils.utf8Buffer(text));
  }

  public void send(String contentType, String body) {
    contentType(contentType);
    send(body);
  }

  public void send(byte[] bytes) {
    ByteBuf buffer = IoUtils.byteBuf(bytes);
    send(buffer);
  }

  public void send(String contentType, byte[] bytes) {
    ByteBuf buffer = IoUtils.byteBuf(bytes);
    send(contentType, buffer);
  }

  @Override
  public void send(InputStream inputStream) throws IOException {
    ByteBuf buffer = bufferAllocator.buffer();
    try {
      send(IoUtils.writeTo(inputStream, buffer));
    } finally {
      buffer.release();
    }
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

    if (!contentLengthSet) {
      headers.set(HttpHeaders.Names.CONTENT_LENGTH, buffer.writerIndex());
    }

    response.content().writeBytes(buffer);
    commit();
  }

  public void sendFile(Blocking blocking, String contentType, File file) {
    contentType(contentType);
    setCookieHeader();
    new FileHttpTransmitter().transmit(blocking, file, request, response, channel);
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
    boolean shouldClose = true;
    setCookieHeader();
    if (channel.isOpen()) {
      if (keepAlive && contentLengthSet) {
        if (version == HttpVersion.HTTP_1_0) {
          headers.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        shouldClose = false;
      }
      ChannelFuture future = channel.writeAndFlush(response);
      if (shouldClose) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }
}
