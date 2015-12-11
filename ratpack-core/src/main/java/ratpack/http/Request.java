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

import com.google.common.net.HostAndPort;
import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.cookie.Cookie;
import ratpack.api.Nullable;
import ratpack.exec.Promise;
import ratpack.func.Block;
import ratpack.registry.MutableRegistry;
import ratpack.server.ServerConfig;
import ratpack.util.MultiValueMap;
import ratpack.util.Types;

import java.time.Instant;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A request to be handled.
 */
public interface Request extends MutableRegistry {

  /**
   * A type token for this type.
   *
   * @since 1.1
   */
  TypeToken<Request> TYPE = Types.token(Request.class);

  /**
   * The method of the request.
   *
   * @return The method of the request.
   */
  HttpMethod getMethod();

  /**
   * The HTTP protocol of the request.
   *
   * @return The HTTP protocol of the request.
   */
  String getProtocol();

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
   * If this request does not have a body, a non null object is still returned but it effectively has no data.
   * <p>
   * If the transmitted content is larger than provided {@link ServerConfig#getMaxContentLength()}, the given block will be invoked.
   * If the block completes successfully, the promise will be terminated.
   * If the block errors, the promise will carry the failure.
   *
   * @return the body of the request
   */
  Promise<TypedData> getBody();

  /**
   * The body of the request.
   * <p>
   * If this request does not have a body, a non null object is still returned but it effectively has no data.
   * <p>
   * If the transmitted content is larger than provided {@code maxContentLength}, the given block will be invoked.
   * If the block completes successfully, the promise will be terminated.
   * If the block errors, the promise will carry the failure.
   *
   * @param onTooLarge the action to take if the request body exceeds the given maxContentLength
   * @return the body of the request
   * @since 1.1
   */
  Promise<TypedData> getBody(Block onTooLarge);

  /**
   * The body of the request allowing up to the provided size for the content.
   * <p>
   * If this request does not have a body, a non null object is still returned but it effectively has no data.
   * <p>
   * If the transmitted content is larger than the provided {@code maxContentLength}, an {@code 413} client error will be issued.
   *
   * @param maxContentLength the maximum number of bytes allowed for the request.
   * @return the body of the request.
   * @since 1.1
   */
  Promise<TypedData> getBody(long maxContentLength);

  /**
   * The body of the request allowing up to the provided size for the content.
   * <p>
   * If this request does not have a body, a non null object is still returned but it effectively has no data.
   * <p>
   * If the transmitted content is larger than the provided {@code maxContentLength}, the given block will be invoked.
   * If the block completes successfully, the promise will be terminated.
   * If the block errors, the promise will carry the failure.
   *
   * @param maxContentLength the maximum number of bytes allowed for the request.
   * @param onTooLarge the action to take if the request body exceeds the given maxContentLength
   * @return the body of the request.
   * @since 1.1
   */
  Promise<TypedData> getBody(long maxContentLength, Block onTooLarge);

  /**
   * The request headers.
   *
   * @return The request headers.
   */
  Headers getHeaders();

  /**
   * The type of the data as specified in the {@code "content-type"} header.
   * <p>
   * If no {@code "content-type"} header is specified, an empty {@link MediaType} is returned.
   *
   * @return The type of the data.
   * @see ratpack.http.MediaType#isEmpty()
   */
  MediaType getContentType();

  /**
   * The address of the client that initiated the request.
   *
   * @return the address of the client that initiated the request
   */
  HostAndPort getRemoteAddress();

  /**
   * The address of the local network interface that received the request.
   *
   * @return the address of the network interface that received the request
   */
  HostAndPort getLocalAddress();

  /**
   * A flag representing whether or not the request originated via AJAX.
   * @return A flag representing whether or not the request originated via AJAX.
   */
  boolean isAjaxRequest();

  /**
   * The timestamp for when this request was received.
   * Specifically, this is the timestamp of creation of the request object.
   *
   * @return the instant timestamp for the request.
   */
  Instant getTimestamp();

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Request add(Class<? super O> type, O object);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Request add(TypeToken<? super O> type, O object);

  /**
   * {@inheritDoc}
   */
  @Override
  Request add(Object object);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Request addLazy(Class<O> type, Supplier<? extends O> supplier);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Request addLazy(TypeToken<O> type, Supplier<? extends O> supplier);

}
