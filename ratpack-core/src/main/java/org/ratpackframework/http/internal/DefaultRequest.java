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
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.*;
import org.ratpackframework.http.HttpMethod;
import org.ratpackframework.http.MediaType;
import org.ratpackframework.http.Request;
import org.ratpackframework.util.MultiValueMap;
import org.ratpackframework.util.internal.ImmutableDelegatingMultiValueMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class DefaultRequest implements Request {

  private final FullHttpRequest nettyRequest;

  private MediaType mediaType;

  private ImmutableDelegatingMultiValueMap<String, String> queryParams;
  private ImmutableDelegatingMultiValueMap<String, String> form;
  private String query;
  private String path;
  private final HttpMethod method;
  private Set<Cookie> cookies;

  public DefaultRequest(FullHttpRequest nettyRequest) {
    this.nettyRequest = nettyRequest;
    this.method = new DefaultHttpMethod(nettyRequest.getMethod().name());
  }

  public MultiValueMap<String, String> getQueryParams() {
    if (queryParams == null) {
      QueryStringDecoder queryStringDecoder = new QueryStringDecoder(getUri());
      queryParams = new ImmutableDelegatingMultiValueMap<>(queryStringDecoder.parameters());
    }
    return queryParams;
  }

  public MediaType getContentType() {
    if (mediaType == null) {
      mediaType = DefaultMediaType.get(getHeader(HttpHeaders.Names.CONTENT_TYPE));
    }
    return mediaType;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public String getUri() {
    return nettyRequest.getUri();
  }

  public String getQuery() {
    if (query == null) {
      String uri = getUri();
      int i = uri.indexOf("?");
      if (i < 0 || i == uri.length()) {
        query = "";
      } else {
        query = uri.substring(i + 1);
      }
    }

    return query;
  }

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

  public String getText() {
    return getBuffer().toString(Charset.forName(getContentType().getCharset()));
  }

  @Override
  public byte[] getBytes() {
    ByteBuf buffer = getBuffer();
    if (buffer.hasArray()) {
      return buffer.array();
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(buffer.writerIndex());
      try {
        writeBodyTo(baos);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return baos.toByteArray();
    }
  }

  @Override
  public void writeBodyTo(OutputStream destination) throws IOException {
    ByteBuf buffer = getBuffer();
    buffer.resetReaderIndex();
    buffer.readBytes(destination, buffer.writerIndex());
  }

  @Override
  public InputStream getInputStream() {
    return new ByteBufInputStream(getBuffer());
  }

  private ByteBuf getBuffer() {
    return nettyRequest.content();
  }

  public MultiValueMap<String, String> getForm() {
    if (form == null) {
      QueryStringDecoder formDecoder = new QueryStringDecoder(getText(), false);
      form = new ImmutableDelegatingMultiValueMap<>(formDecoder.parameters());
    }
    return form;
  }

  public Set<Cookie> getCookies() {
    if (cookies == null) {
      String header = nettyRequest.headers().get(HttpHeaders.Names.COOKIE);
      if (header == null || header.length() == 0) {
        cookies = Collections.emptySet();
      } else {
        cookies = CookieDecoder.decode(header);
      }
    }

    return cookies;
  }

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

  public String getHeader(String name) {
    return nettyRequest.headers().get(name);
  }

  public Date getDateHeader(String name) {
    return HttpHeaders.getDateHeader(nettyRequest, name, null);
  }

  public List<String> getHeaders(String name) {
    return nettyRequest.headers().getAll(name);
  }

  public boolean containsHeader(String name) {
    return nettyRequest.headers().contains(name);
  }

  public Set<String> getHeaderNames() {
    return nettyRequest.headers().names();
  }
}
