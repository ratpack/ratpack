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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.http.HttpExchange;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;

public class DefaultHttpExchange implements HttpExchange {

  private final HttpRequest request;
  private final HttpResponse response;
  private final ChannelHandlerContext channelHandlerContext;
  private final Handler<ErroredHttpExchange> errorHandler;
  private File targetFile;

  private Set<Cookie> incomingCookies;
  private Set<Cookie> outgoingCookies;

  private String query;
  private String path;

  public DefaultHttpExchange(File targetFile, HttpRequest request, HttpResponse response, ChannelHandlerContext channelHandlerContext, Handler<ErroredHttpExchange> errorHandler) {
    this.targetFile = targetFile;
    this.request = request;
    this.response = response;
    this.channelHandlerContext = channelHandlerContext;
    this.errorHandler = errorHandler;
  }

  @Override
  public File getTargetFile() {
    return targetFile;
  }

  @Override
  public void setTargetFile(File targetFile) {
    this.targetFile = targetFile;
  }

  public HttpRequest getRequest() {
    return request;
  }

  public HttpResponse getResponse() {
    return response;
  }

  @Override
  public void end(String contentType, String content) {
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType);
    end(ChannelBuffers.copiedBuffer(content, CharsetUtil.UTF_8));
  }

  @Override
  public void end(ChannelBuffer channelBuffer) {
    response.setContent(channelBuffer);
    complete();
  }

  private void complete() {
    if (outgoingCookies != null && !outgoingCookies.isEmpty()) {
      for (Cookie cookie : outgoingCookies) {
        CookieEncoder cookieEncoder = new CookieEncoder(true);
        cookieEncoder.addCookie(cookie);
        response.addHeader(SET_COOKIE, cookieEncoder.encode());
      }
    }

    Channel channel = getChannel();
    if (channel.isOpen()) {
      ChannelFuture future = channel.write(response);
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  public Channel getChannel() {
    return channelHandlerContext.getChannel();
  }

  @Override
  public void end() {
    end(ChannelBuffers.EMPTY_BUFFER);
  }

  @Override
  public void end(HttpResponseStatus status) {
    response.setStatus(status);
    end();
  }

  @Override
  public void error(Exception e) {
    errorHandler.handle(new ErroredHttpExchange(this, e));
  }

  @Override
  public Set<Cookie> getIncomingCookies() {
    if (incomingCookies == null) {
      String header = request.getHeader(HttpHeaders.Names.COOKIE);
      if (header == null || header.length() == 0) {
        incomingCookies = Collections.emptySet();
      } else {
        incomingCookies = new CookieDecoder().decode(header);
      }
    }
    return incomingCookies;
  }

  @Override
  public Set<Cookie> getOutgoingCookies() {
    if (outgoingCookies == null) {
      outgoingCookies = new HashSet<>();
    }
    return outgoingCookies;
  }

  @Override
  public String getUri() {
    return getRequest().getUri();
  }

  @Override
  public String getQuery() {
    if (query == null) {
      String uri = getUri();
      int i = uri.indexOf("?");
      if (i < 0 || i == uri.length()) {
        query = null;
      } else {
        query = uri.substring(i + 1);
      }
    }

    return query;
  }

  @Override
  public String getPath() {
    if (path == null) {
      String uri = getUri();
      int i = uri.indexOf("?");
      if (i <= 0) {
        path = uri;
      } else {
        path = uri.substring(0, i);
      }
    }

    return path;
  }


}
