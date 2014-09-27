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

package ratpack.http;

import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.Cookie;
import ratpack.api.Nullable;
import ratpack.registry.MutableRegistry;
import ratpack.util.MultiValueMap;

import java.util.Set;
import java.util.function.Supplier;

/**
 * A request to be handled.
 */
public interface Request extends MutableRegistry {

  /**
   * The method of the request.
   *
   * @return The method of the request.
   */
  HttpMethod getMethod();

  /**
   * The raw URI of the request.
   * <p>
   * This value may be an absolute URI or an absolute path.
   *
   * @return The raw URI of the request.
   */
  String getRawUri();

  /**
   * The complete URI of the request (path + query string).
   * <p>
   * This value is always absolute (i.e. begins with "{@code /}").
   *
   * @return The complete URI of the request (path + query string).
   */
  String getUri();

  /**
   * The query string component of the request URI, without the "?".
   * <p>
   * If the request does not contain a query component, an empty string will be returned.
   *
   * @return The query string component of the request URI, without the "?".
   */
  String getQuery();

  /**
   * The URI without the query string and leading forward slash.
   *
   * @return The URI without the query string and leading forward slash
   */
  String getPath();

  /**
   * TBD.
   *
   * @return TBD.
   */
  MultiValueMap<String, String> getQueryParams();

  /**
   * The cookies that were sent with the request.
   * <p>
   * An empty set will be returned if no cookies were sent.
   *
   * @return The cookies that were sent with the request.
   */
  Set<Cookie> getCookies();

  /**
   * Returns the value of the cookie with the specified name if it was sent.
   * <p>
   * If there is more than one cookie with this name, this method will throw an exception.
   *
   * @param name The name of the cookie to get the value of
   * @return The cookie value, or null if not present
   */
  @Nullable
  String oneCookie(String name);

  /**
   * The body of the request.
   * <p>
   * If this request does not have a body, an non null object is still returned but it effectively has no data.
   *
   * @return the body of the request
   */
  TypedData getBody();

  /**
   * The request headers.
   *
   * @return The request headers.
   */
  Headers getHeaders();

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> Request add(Class<? super O> type, O object) {
    MutableRegistry.super.add(type, object);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> Request add(TypeToken<? super O> type, O object) {
    MutableRegistry.super.add(type, object);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default Request add(Object object) {
    MutableRegistry.super.add(object);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> Request addLazy(Class<O> type, Supplier<? extends O> supplier) {
    MutableRegistry.super.addLazy(type, supplier);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Request addLazy(TypeToken<O> type, Supplier<? extends O> supplier);
}
