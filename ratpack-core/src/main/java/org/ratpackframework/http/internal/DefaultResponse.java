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
import java.util.regex.Pattern;

public class DefaultResponse implements Response {

  private final FullHttpResponse response;
  private final Channel channel;
  private final boolean keepAlive;
  private final HttpVersion version;
  private final FullHttpRequest request;
  private boolean contentLengthSet;

  private Set<Cookie> cookies;

  public DefaultResponse(FullHttpResponse response, Channel channel, boolean keepAlive, HttpVersion version, FullHttpRequest request) {
    this.response = response;
    this.channel = channel;
    this.keepAlive = keepAlive;
    this.version = version;
    this.request = request;
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

  protected Response contentType(String contentType) {
    setHeader(HttpHeaders.Names.CONTENT_TYPE, DefaultMediaType.utf8(contentType).toString());
    return this;
  }

  public void send(String contentType, String body) {
    contentType(contentType);
    ByteBuf buffer = IoUtils.utf8Buffer(body);
    if (!contentLengthSet) {
      setHeader(HttpHeaders.Names.CONTENT_LENGTH, buffer.writerIndex());
    }
    response.content().writeBytes(buffer);
    commit();
  }

  public void send(String text) {
    send("text/plain", text);
  }

  public void send(String contentType, ByteBuf buffer) {
    contentType(contentType);
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

  public void redirect(String location) {
    response.setStatus(HttpResponseStatus.FOUND);
    setHeader(HttpHeaders.Names.LOCATION, generateRedirectLocation(location));
    commit();
  }

  public void redirect(int code, String location) {
    status(code);
    setHeader(HttpHeaders.Names.LOCATION, generateRedirectLocation(location));
    commit();
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
    response.headers().add(name, value);
  }

  public void setHeader(String name, Object value) {
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
      contentLengthSet = true;
    }
    response.headers().set(name, value);
  }

  public void setHeader(String name, Iterable<?> values) {
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
      contentLengthSet = true;
    }
    response.headers().set(name, values);
  }

  public void removeHeader(String name) {
    if (name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)) {
      contentLengthSet = false;
    }
    response.headers().remove(name);
  }

  public void clearHeaders() {
    contentLengthSet = false;
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
          response.headers().set("Connection", "Keep-Alive");
        }
        shouldClose = false;
      }
      ChannelFuture future = channel.write(response);
      if (shouldClose) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  private String generateRedirectLocation(String path) {
    //Rules
    //1. Given absolute URL use it
    //2. Given Starting Slash prepend public facing domain:port if provided if not use base URL of request
    //3. Given relative URL prepend public facing domain:port plus parent path of request URL otherwise full parent path

    String generatedPath = null;

    Pattern pattern = Pattern.compile("^https?://.*");

    if (pattern.matcher(path).matches()) {
      //Rule 1 - Path is absolute
      generatedPath = path;
    } else {
      if (path.charAt(0) == '/') {
        //Rule 2 - Starting Slash
        generatedPath = getHost() + path;
      } else {
        //Rule 3
        generatedPath = getHost() + getParentPath(request.getUri()) + path;
      }
    }

    return generatedPath;
  }


  /**
   * Using any specified public url first and then falling back to the Host header. If there is no host header we will return an empty string.
   *
   * @return The host if it can be found
   */
  private String getHost() {
    boolean hasPublicHost = false;

    String host = "";
    if (hasPublicHost) {
      host = "http://publichost.com";
    } else {
      if (request != null) {
        if (request.headers().get("Host") != null) {
          //TODO find if there is a way not to assume http
          host = "http://" + request.headers().get("Host");
        }
      }
    }

    return host;
  }

  private String getParentPath(String path) {
    String parentPath = "/";

    int indexOfSlash = path.lastIndexOf('/');
    if (indexOfSlash >= 0) {
      parentPath = path.substring(0, indexOfSlash) + '/';
    }

    if (!parentPath.startsWith("/")) {
      parentPath = "/" + parentPath;
    }
    return parentPath;
  }

}
