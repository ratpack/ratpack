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
import org.jboss.netty.handler.codec.http.*;
import org.ratpackframework.http.HttpMethod;
import org.ratpackframework.http.MediaType;
import org.ratpackframework.http.Request;

import java.nio.charset.Charset;
import java.util.*;

public class DefaultRequest implements Request {

  private final HttpRequest nettyRequest;

  private MediaType mediaType;

  private Map<String, List<String>> queryParams;
  private String query;
  private String path;
  private final HttpMethod method;
  private Set<Cookie> cookies;

  public DefaultRequest(HttpRequest nettyRequest) {
    this.nettyRequest = nettyRequest;
    this.method = new DefaultHttpMethod(nettyRequest.getMethod().getName());
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
  public MediaType getContentType() {
    if (mediaType == null) {
      mediaType = new MediaType(nettyRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE));
    }
    return mediaType;
  }

  @Override
  public HttpMethod getMethod() {
    return method;
  }

  @Override
  public String getUri() {
    return nettyRequest.getUri();
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
      String noSlash = uri.substring(1);
      int i = noSlash.indexOf("?");
      if (i < 0) {
        path = noSlash;
      } else {
        path = noSlash.substring(0, i);
      }
    }

    return path;
  }

  @Override
  public String getText() {
    return getBuffer().toString(Charset.forName(getContentType().getCharset()));
  }

  private ChannelBuffer getBuffer() {
    return nettyRequest.getContent();
  }

  @Override
  public Map<String, List<String>> getForm() {
    return new QueryStringDecoder(getText(), false).getParameters();
  }

  @Override
  public Set<Cookie> getCookies() {
    if (cookies == null) {
      String header = nettyRequest.getHeader(HttpHeaders.Names.COOKIE);
      if (header == null || header.length() == 0) {
        cookies = Collections.emptySet();
      } else {
        cookies = new CookieDecoder().decode(header);
      }
    }

    return cookies;
  }

  @Override
  public String oneCookie(String name) {
    Cookie found = null;
    List<Cookie> allFound = null;
    for (Cookie cookie : getCookies()) {
      if (cookie.getName().equals(name)) {
        if (found == null) {
          found = cookie;
        } else if (allFound == null) {
          allFound = new ArrayList<>(2);
          allFound.add(found);
        } else {
          allFound.add(cookie);
        }
      }
    }

    if (found == null) {
      return null;
    } else if (allFound != null) {
      StringBuilder s = new StringBuilder("Multiple cookies with name '").append(name).append("': ");
      int i = 0;
      for (Cookie cookie : allFound) {
        s.append(cookie.toString());
        if (++i < allFound.size()) {
          s.append(", ");
        }
      }

      throw new IllegalStateException(s.toString());
    } else {
      return found.getValue();
    }
  }

  @Override
  public String getHeader(String name) {
    return nettyRequest.getHeader(name);
  }

  @Override
  public List<String> getHeaders(String name) {
    return nettyRequest.getHeaders(name);
  }

  @Override
  public List<Map.Entry<String, String>> getHeaders() {
    return nettyRequest.getHeaders();
  }

  @Override
  public boolean containsHeader(String name) {
    return nettyRequest.containsHeader(name);
  }

  @Override
  public Set<String> getHeaderNames() {
    return nettyRequest.getHeaderNames();
  }
}
