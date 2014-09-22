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

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.http.Headers;
import ratpack.http.HttpMethod;
import ratpack.http.Request;
import ratpack.http.TypedData;
import ratpack.registry.MutableRegistry;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.internal.SimpleMutableRegistry;
import ratpack.util.MultiValueMap;
import ratpack.util.internal.ImmutableDelegatingMultiValueMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DefaultRequest implements Request {

  private final MutableRegistry registry = new SimpleMutableRegistry();

  private final Headers headers;
  private final ByteBuf content;
  private final String rawUri;

  private TypedData body;

  private String uri;
  private ImmutableDelegatingMultiValueMap<String, String> queryParams;
  private String query;
  private String path;
  private final HttpMethod method;
  private Set<Cookie> cookies;

  public DefaultRequest(Headers headers, String methodName, String rawUri, ByteBuf content) {
    this.headers = headers;
    this.content = content;
    this.method = new DefaultHttpMethod(methodName);
    this.rawUri = rawUri;
  }

  public MultiValueMap<String, String> getQueryParams() {
    if (queryParams == null) {
      QueryStringDecoder queryStringDecoder = new QueryStringDecoder(getUri());
      queryParams = new ImmutableDelegatingMultiValueMap<>(queryStringDecoder.parameters());
    }
    return queryParams;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public String getRawUri() {
    return rawUri;
  }

  public String getUri() {
    if (uri == null) {
      if (rawUri.startsWith("/")) {
        uri = rawUri;
      } else {
        URI parsed = URI.create(rawUri);
        String path = parsed.getPath();
        if (Strings.isNullOrEmpty(path)) {
          path = "/";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        if (parsed.getQuery() != null) {
          sb.append("?").append(parsed.getQuery());
        }
        if (parsed.getFragment() != null) {
          sb.append("#").append(parsed.getFragment());
        }
        uri = sb.toString();
      }
    }
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
  public TypedData getBody() {
    if (body == null) {
      body = new ByteBufBackedTypedData(content, DefaultMediaType.get(headers.get(HttpHeaders.Names.CONTENT_TYPE)));
    }
    return body;
  }

  @Override
  public Headers getHeaders() {
    return headers;
  }

  @Override
  public <O> void register(Class<O> type, O object) {
    registry.register(type, object);
  }

  @Override
  public void register(Object object) {
    registry.register(object);
  }

  @Override
  public <O> void registerLazy(Class<O> type, Factory<? extends O> factory) {
    registry.registerLazy(type, factory);
  }

  @Override
  public <O> void remove(Class<O> type) throws NotInRegistryException {
    registry.remove(type);
  }

  @Override
  @Nullable
  public <O> O maybeGet(TypeToken<O> type) {
    return registry.maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return registry.getAll(type);
  }

  @Nullable
  @Override
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.first(type, predicate);
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.all(type, predicate);
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    return registry.each(type, predicate, action);
  }
}
