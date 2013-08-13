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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.ratpackframework.file.internal.FileHttpTransmitter;
import org.ratpackframework.http.Response;
import org.ratpackframework.util.internal.IoUtils;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultResponse implements Response {

  private final FullHttpResponse response;
  private final Channel channel;
  private final boolean keepAlive;
  private final HttpVersion version;
  private boolean contentLengthSet;
  private boolean contentTypeSet;

  private Set<Cookie> cookies;

  public DefaultResponse(FullHttpResponse response, Channel channel, boolean keepAlive, HttpVersion version) {
    this.response = response;
    this.channel = channel;
    this.keepAlive = keepAlive;
    this.version = version;
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

  public void send() {
    contentLengthSet = true;
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0);
    commit();
  }

  @Override
  public Response contentType(String contentType) {
    setHeader(HttpHeaders.Names.CONTENT_TYPE, DefaultMediaType.utf8(contentType).toString());
    return this;
  }

  public void send(String contentType, String body) {
    contentType(contentType);
    doSend(body);
  }

  private void doSend(String body) {
    ByteBuf buffer = IoUtils.utf8Buffer(body);
    if (!contentLengthSet) {
      setHeader(HttpHeaders.Names.CONTENT_LENGTH, buffer.writerIndex());
    }
    response.content().writeBytes(buffer);
    commit();
  }

  public void send(String text) {
    if (!contentTypeSet) {
      send("text/plain", text);
    } else {
      doSend(text);
    }
  }

  public void send(byte[] bytes) {
    ByteBuf buffer = IoUtils.byteBuf(bytes);
    send(buffer);
  }

  public void send(String contentType, byte[] bytes) {
    ByteBuf buffer = IoUtils.byteBuf(bytes);
    send(contentType, buffer);
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
      setHeader(HttpHeaders.Names.CONTENT_LENGTH, buffer.writerIndex());
    }

    response.content().writeBytes(buffer);
    commit();
  }

  public void sendFile(String contentType, File file) {
    contentType(contentType);
    setCookieHeader();
    new FileHttpTransmitter().transmit(file, response, channel);
  }

  public String getHeader(String name) {
    return response.headers().get(name);
  }

  public List<String> getHeaders(String name) {
    return response.headers().getAll(name);
  }

  public boolean containsHeader(String name) {
    return response.headers().contains(name);
  }

  public Set<String> getHeaderNames() {
    return response.headers().names();
  }

  public void addHeader(String name, Object value) {
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
      contentLengthSet = true;
    }
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
      contentTypeSet = true;
    }
    response.headers().add(name, value);
  }

  public void setHeader(String name, Object value) {
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
      contentLengthSet = true;
    }
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
      contentTypeSet = true;
    }
    response.headers().set(name, value);
  }

  public void setHeader(String name, Iterable<?> values) {
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
      contentLengthSet = true;
    }
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
      contentTypeSet = true;
    }
    response.headers().set(name, values);
  }

  public void removeHeader(String name) {
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
      contentLengthSet = false;
    }
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_TYPE)) {
      contentTypeSet = false;
    }
    response.headers().remove(name);
  }

  public void clearHeaders() {
    contentLengthSet = false;
    contentTypeSet = false;
    response.headers().clear();
  }

  public Set<Cookie> getCookies() {
    if (cookies == null) {
      cookies = new HashSet<Cookie>();
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
        response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));
      }
    }
  }

  private void commit() {
    boolean shouldClose = true;
    setCookieHeader();
    if (channel.isOpen()) {
      if (keepAlive && contentLengthSet) {
        if (version == HttpVersion.HTTP_1_0) {
          response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
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
