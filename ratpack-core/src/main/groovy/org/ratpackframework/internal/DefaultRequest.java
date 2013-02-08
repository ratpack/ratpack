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

package org.ratpackframework.internal;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.ratpackframework.Request;
import org.ratpackframework.handler.HttpExchange;
import org.ratpackframework.http.MediaType;
import org.ratpackframework.session.Session;
import org.ratpackframework.session.internal.RequestSessionManager;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class DefaultRequest implements Request {

  private final RequestSessionManager sessionManager;
  private final HttpExchange exchange;
  private MediaType mediaType;

  private final Map<String, String> urlParams;
  private Map<String, List<String>> queryParams;
  private String query;
  private String path;

  public DefaultRequest(HttpExchange exchange, Map<String, String> urlParams, RequestSessionManager sessionManager) {
    this.exchange = exchange;
    this.urlParams = urlParams;
    this.sessionManager = sessionManager;
  }

  @Override
  public Map<String, List<String>> getQueryParams() {
    if (queryParams == null) {
      QueryStringDecoder queryStringDecoder = new QueryStringDecoder(getUri());
      queryParams = queryStringDecoder.getParameters();
    }
    return queryParams;
  }

  @Override
  public Map<String, String> getUrlParams() {
    return urlParams;
  }

  @Override
  public MediaType getContentType() {
    if (mediaType == null) {
      mediaType = new MediaType(exchange.getRequest().getHeader(HttpHeaders.Names.CONTENT_TYPE));
    }
    return mediaType;
  }

  @Override
  public String getMethod() {
    return exchange.getRequest().getMethod().getName();
  }

  @Override
  public String getUri() {
    return exchange.getRequest().getUri();
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

  @Override
  public Session getSession() {
    return sessionManager.getSession();
  }

  @Override
  public String getText() {
    return getBuffer().toString(Charset.forName(getContentType().getCharset()));
  }

  @Override
  public ChannelBuffer getBuffer() {
    return exchange.getRequest().getContent();
  }

  @Override
  public Map<String, List<String>> getForm() {
    return new QueryStringDecoder(getText(), false).getParameters();
  }
}
