/*
 * Copyright 2012 the original author or authors.
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.*;
import org.ratpackframework.http.Response;
import org.ratpackframework.http.MediaType;
import org.ratpackframework.file.internal.FileHttpTransmitter;
import org.ratpackframework.util.IoUtils;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultResponse implements Response {

  private final HttpResponse response;
  private final Channel channel;

  private Set<Cookie> cookies;

  public DefaultResponse(HttpResponse response, Channel channel) {
    this.response = response;
    this.channel = channel;
  }

  @Override
  public List<Map.Entry<String, String>> getHeaders() {
    return response.getHeaders();
  }

  @Override
  public Status getStatus() {
    return new Status() {
      @Override
      public int getCode() {
        return response.getStatus().getCode();
      }

      @Override
      public String getMessage() {
        return response.getStatus().getReasonPhrase();
      }
    };
  }

  @Override
  public Response status(int code) {
    response.setStatus(HttpResponseStatus.valueOf(code));
    return this;
  }

  @Override
  public Response status(int code, String message) {
    response.setStatus(new HttpResponseStatus(code, message));
    return this;
  }

  @Override
  public void send() {
    commit();
  }

  protected Response contentType(String contentType) {
    setHeader(HttpHeaders.Names.CONTENT_TYPE, new MediaType(contentType, "utf8").toString());
    return this;
  }

  @Override
  public void send(String contentType, String str) {
    contentType(contentType);
    response.setContent(IoUtils.utf8Buffer(str));
    commit();
  }

  @Override
  public void send(String text) {
    send("text/plain", text);
  }

  @Override
  public void send(String contentType, ChannelBuffer buffer) {
    contentType(contentType);
    response.setContent(buffer);
    commit();
  }

  @Override
  public void sendFile(String contentType, File file) {
    contentType(contentType);
    setCookieHeader();
    new FileHttpTransmitter().transmit(file, response, channel);
  }

  public void redirect(String location) {
    response.setStatus(HttpResponseStatus.FOUND);
    setHeader(HttpHeaders.Names.LOCATION, location);
    commit();
  }

  @Override
  public void redirect(int code, String location) {
    status(code);
    setHeader(HttpHeaders.Names.LOCATION, location);
    commit();
  }

  @Override
  public String getHeader(String name) {
    return response.getHeader(name);
  }

  @Override
  public List<String> getHeaders(String name) {
    return response.getHeaders(name);
  }

  @Override
  public boolean containsHeader(String name) {
    return response.containsHeader(name);
  }

  @Override
  public Set<String> getHeaderNames() {
    return response.getHeaderNames();
  }

  @Override
  public void addHeader(String name, Object value) {
    response.addHeader(name, value);
  }

  @Override
  public void setHeader(String name, Object value) {
    response.setHeader(name, value);
  }

  @Override
  public void setHeader(String name, Iterable<?> values) {
    response.setHeader(name, values);
  }

  @Override
  public void removeHeader(String name) {
    response.removeHeader(name);
  }

  @Override
  public void clearHeaders() {
    response.clearHeaders();
  }

  @Override
  public Set<Cookie> getCookies() {
    if (cookies == null) {
      cookies = new HashSet<>();
    }
    return cookies;
  }

  @Override
  public Cookie cookie(String name, String value) {
    Cookie cookie = new DefaultCookie(name, value);
    getCookies().add(cookie);
    return cookie;
  }

  @Override
  public Cookie expireCookie(String name) {
    Cookie cookie = cookie(name, "");
    cookie.setMaxAge(0);
    return cookie;
  }

  private void setCookieHeader() {
    if (cookies != null && !cookies.isEmpty()) {
      for (Cookie cookie : cookies) {
        CookieEncoder cookieEncoder = new CookieEncoder(true);
        cookieEncoder.addCookie(cookie);
        response.addHeader(HttpHeaders.Names.SET_COOKIE, cookieEncoder.encode());
      }
    }
  }

  private void commit() {
    setCookieHeader();
    if (channel.isOpen()) {
      ChannelFuture future = channel.write(response);
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  public HttpResponse getNettyResponse() {
    return response;
  }

}
