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

package ratpack.test.handling;

import io.netty.handler.codec.http.cookie.Cookie;
import ratpack.api.Nullable;
import ratpack.http.Headers;
import ratpack.http.Status;
import ratpack.registry.Registry;

import java.nio.file.Path;
import java.util.Set;

/**
 * Represents the result of testing one or more handlers.
 *
 * @see ratpack.test.handling.RequestFixture
 */
public interface HandlingResult {

  /**
   * The response body, as bytes.
   * <p>
   * This does not include file or rendered responses.
   * See {@link #getSentFile()} and {@link #rendered(Class)}.
   *
   * @return the response body, as bytes, or {@code null} if no response was sent.
   */
  @Nullable
  byte[] getBodyBytes();

  /**
   * The response body, interpreted as a utf8 string.
   * <p>
   * This does not include file or rendered responses.
   * See {@link #getSentFile()} and {@link #rendered(Class)}.
   *
   * @return the response body, interpreted as a utf8 string, or {@code null} if no response was sent.
   */
  @Nullable
  String getBodyText();

  /**
   * The cookies to be set as part of the response.
   * <p>
   * Cookies are set during request processing via the {@link ratpack.http.Response#cookie(String, String)} method,
   * or via directly modifying {@link ratpack.http.Response#getCookies()}.
   *
   * @return the cookies to be set as part of the response
   * @since 1.3
   */
  @Nullable
  Set<Cookie> getCookies();

  /**
   * The client error raised if any, unless a custom client error handler is in use.
   * <p>
   * If no client error was “raised”, will be {@code null}.
   * <p>
   * If a custom client error handler is used (either by specification in the request fixture or insertion by an upstream handler), this will always be {@code null}.
   * In such a case, this result effectively indicates what the custom client error handler did as its implementation.
   *
   * @return the client error code, or {@code null}.
   */
  @Nullable
  Integer getClientError();

  /**
   * The throwable thrown or given to {@link ratpack.handling.Context#error(Throwable)}, unless a custom error handler is in use.
   * <p>
   * If no throwable was “raised”, a new {@link ratpack.test.handling.HandlerExceptionNotThrownException} is raised.
   * <p>
   * If a custom error handler is used (either by specification in the request fixture or insertion by an upstream handler),
   * this will always raise a new {@link ratpack.test.handling.HandlerExceptionNotThrownException}
   * In such a case, this result effectively indicates what the custom error handler did as its implementation.
   *
   * @param type The expected type of the exception captured.
   * @param <T> The expected type of the exception captured.
   * @return the “unhandled” throwable that occurred, or raise {@link ratpack.test.handling.HandlerExceptionNotThrownException}
   */
  <T extends Throwable> T exception(Class<T> type);

  /**
   * The final response headers.
   *
   * @return the final response headers
   */
  Headers getHeaders();

  /**
   * The final state of the context registry.
   *
   * @return the final state of the context registry
   */
  Registry getRegistry();

  /**
   * The final state of the request registry.
   *
   * @return the final state of the reqest registry
   */
  Registry getRequestRegistry();

  /**
   * Indicates whether the result of invoking the handler was that it invoked one of the {@link ratpack.http.Response#sendFile} methods.
   * <p>
   * This does not include files rendered with {@link ratpack.handling.Context#render(Object)}.
   *
   * @return the file given to one of the {@link ratpack.http.Response#sendFile} methods, or {@code null} if none of these methods were called
   */
  @Nullable
  Path getSentFile();

  /**
   * The response status information.
   * <p>
   * Indicates the state of the context's {@link ratpack.http.Response#getStatus()} after invoking the handler.
   * If the result is a sent response, this indicates the status of the response.
   *
   * @return the response status
   */
  Status getStatus();

  /**
   * Indicates whether the result of invoking the handler was that it delegated to a downstream handler.
   *
   * @return whether the handler delegated to a downstream handler
   */
  boolean isCalledNext();

  /**
   * Indicates the the handler(s) invoked one of the {@link ratpack.http.Response#send} methods.
   *
   * @return whether one of the {@link ratpack.http.Response#send} methods was invoked
   */
  boolean isSentResponse(); // This is not named right, as it doesn't include sending files

  /**
   * The object that was rendered to the response.
   * <p>
   * The exact object that was given to {@link ratpack.handling.Context#render(Object)}.
   * The value must be assignment compatible with given type token.
   * If it is not, an {@link AssertionError} will be thrown.
   *
   * @param type the expect type of the rendered object
   * @param <T> the expect type of the rendered object
   * @return the rendered object, or {@code null} if no object was rendered
   * @throws AssertionError if the rendered object cannot be cast to the given type
   */
  @Nullable
  <T> T rendered(Class<T> type) throws AssertionError;

}
