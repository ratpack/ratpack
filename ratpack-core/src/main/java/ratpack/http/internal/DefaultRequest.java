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

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import ratpack.exec.ExecControl;
import ratpack.exec.Promise;
import ratpack.func.Function;
import ratpack.http.Headers;
import ratpack.http.HttpMethod;
import ratpack.http.Request;
import ratpack.http.TypedData;
import ratpack.registry.MutableRegistry;
import ratpack.registry.NotInRegistryException;
import ratpack.util.MultiValueMap;
import ratpack.util.internal.ImmutableDelegatingMultiValueMap;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public class DefaultRequest implements Request {

  private MutableRegistry registry;

  private final Headers headers;
  private final ByteBuf content;
  private final String rawUri;
  private final HttpMethod method;
  private final String protocol;
  private final InetSocketAddress remoteSocket;
  private final InetSocketAddress localSocket;
  private final Instant timestamp;

  private Promise<TypedData> body;

  private String uri;
  private ImmutableDelegatingMultiValueMap<String, String> queryParams;
  private String query;
  private String path;
  private Set<Cookie> cookies;

  public DefaultRequest(Instant timestamp, Headers headers, io.netty.handler.codec.http.HttpMethod method, HttpVersion protocol, String rawUri, InetSocketAddress remoteSocket, InetSocketAddress localSocket, ByteBuf content) {
    this.headers = headers;
    this.content = content;
    this.method = DefaultHttpMethod.valueOf(method);
    this.protocol = protocol.toString();
    this.rawUri = rawUri;
    this.remoteSocket = remoteSocket;
    this.localSocket = localSocket;
    this.timestamp = timestamp;
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

  public String getProtocol() {
    return protocol;
  }

  public Instant getTimestamp() {
    return timestamp;
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
      String header = headers.get(HttpHeaderNames.COOKIE);
      if (header == null || header.length() == 0) {
        cookies = Collections.emptySet();
      } else {
        cookies = ServerCookieDecoder.STRICT.decode(header);
      }
    }

    return cookies;
  }

  public String oneCookie(String name) {
    Cookie found = null;
    List<Cookie> allFound = null;
    for (Cookie cookie : getCookies()) {
      if (cookie.name().equals(name)) {
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
      return found.value();
    }
  }

  public boolean isAjaxRequest() {
    return HttpHeaderConstants.XML_HTTP_REQUEST.equalsIgnoreCase(headers.get(HttpHeaderConstants.X_REQUESTED_WITH));
  }

  @Override
  public Promise<TypedData> getBody() {
    if (body == null) {
      body = ExecControl.current().promiseOf(new ByteBufBackedTypedData(content, DefaultMediaType.get(headers.get(HttpHeaderNames.CONTENT_TYPE))));
    }
    return body;
  }

  @Override
  public Headers getHeaders() {
    return headers;
  }

  @Override
  public HostAndPort getRemoteAddress() {
    return HostAndPort.fromParts(remoteSocket.getHostString(), remoteSocket.getPort());
  }

  @Override
  public HostAndPort getLocalAddress() {
    return HostAndPort.fromParts(localSocket.getHostString(), localSocket.getPort());
  }

  @Override
  public <O> Request addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    getDelegateRegistry().addLazy(type, supplier);
    return this;
  }

  @Override
  public <O> Request add(TypeToken<? super O> type, O object) {
    getDelegateRegistry().add(type, object);
    return this;
  }

  @Override
  public <O> Request add(Class<? super O> type, O object) {
    getDelegateRegistry().add(type, object);
    return this;
  }

  @Override
  public Request add(Object object) {
    getDelegateRegistry().add(object);
    return this;
  }

  @Override
  public <O> Request addLazy(Class<O> type, Supplier<? extends O> supplier) {
    getDelegateRegistry().addLazy(type, supplier);
    return this;
  }

  @Override
  public <T> void remove(TypeToken<T> type) throws NotInRegistryException {
    getDelegateRegistry().remove(type);
  }

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return getDelegateRegistry().maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return getDelegateRegistry().getAll(type);
  }

  @Override
  public <T, O> Optional<O> first(TypeToken<T> type, Function<? super T, ? extends O> function) throws Exception {
    return getDelegateRegistry().first(type, function);
  }

  private MutableRegistry getDelegateRegistry() {
    if (registry == null) {
      throw new IllegalStateException("Cannot access registry before it has been set");
    }
    return registry;
  }

  // Implemented as static method (instead of public instance) so that it's not accidentally callable from dynamic languages
  public static void setDelegateRegistry(DefaultRequest request, MutableRegistry registry) {
    request.registry = registry;
  }
}
