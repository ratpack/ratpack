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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.ratpackframework.http.Headers;
import org.ratpackframework.http.HttpMethod;
import org.ratpackframework.http.MediaType;
import org.ratpackframework.http.Request;
import org.ratpackframework.util.MultiValueMap;
import org.ratpackframework.util.internal.ImmutableDelegatingMultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.*;

public class DefaultRequest implements Request {

  private final Headers headers;
  private final ByteBuf content;
  private final String uri;

  private MediaType mediaType;

  private ImmutableDelegatingMultiValueMap<String, String> queryParams;
  private ImmutableDelegatingMultiValueMap<String, String> form;
  private String query;
  private String path;
  private final HttpMethod method;
  private Set<Cookie> cookies;

  public DefaultRequest(Headers headers, String methodName, String uri, ByteBuf content) {
    this.headers = headers;
    this.method = new DefaultHttpMethod(methodName);
    this.uri = uri;
    this.content = content;
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
      mediaType = DefaultMediaType.get(headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }
    return mediaType;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public String getUri() {
    return uri;
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
    return content;
  }

  public MultiValueMap<String, String> getForm() {
    if (form == null) {
      form = ContentTypeFormDecoderEngine.getForContentType(getContentType().getType(), getText());
    }
    return form;
  }

  public Set<Cookie> getCookies() {
    if (cookies == null) {
      String header = headers.get(HttpHeaders.Names.COOKIE);
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

  @Override
  public Headers getHeaders() {
    return headers;
  }

  interface ContentTypeFormDecoder {
    ImmutableDelegatingMultiValueMap<String, String> decode(String body);
  }

  private enum ContentTypeFormDecoderEngine implements ContentTypeFormDecoder {
    FORM(MediaType.APPLICATION_FORM) {
      @Override
      public ImmutableDelegatingMultiValueMap<String, String> decode(String body) {
        QueryStringDecoder formDecoder = new QueryStringDecoder(body, false);
        return new ImmutableDelegatingMultiValueMap<>(formDecoder.parameters());
      }
    }, JSON(MediaType.APPLICATION_JSON) {
      @Override
      public ImmutableDelegatingMultiValueMap<String, String> decode(String body) {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<String, String>>  typeRef = new TypeReference<HashMap<String, String>>() {};

        try {
          Map<String, String> jsonMap = objectMapper.readValue(body, typeRef);
          Map<String, List<String>> convertingMap = Maps.newHashMap();
          for (Map.Entry<String, String> entry : jsonMap.entrySet()) {
            convertingMap.put(entry.getKey(), Lists.newArrayList(entry.getValue()));
          }
          return new ImmutableDelegatingMultiValueMap<>(convertingMap);
        } catch (IOException e) {
          return FORM.decode(body);
        }
      }
    };

    String mediaType;

    ContentTypeFormDecoderEngine(String mediaType) {
      this.mediaType = mediaType;
    }

    static ImmutableDelegatingMultiValueMap<String, String> getForContentType(String mediaType, String body) {
      for (ContentTypeFormDecoderEngine decoder : values()) {
        if (decoder.mediaType.equals(mediaType)) {
          return decoder.decode(body);
        }
      }
      return FORM.decode(body);
    }
  }
}
