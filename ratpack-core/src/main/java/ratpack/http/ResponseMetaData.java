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

import io.netty.handler.codec.http.cookie.Cookie;

import java.util.Set;

/**
 * The metadata associated with a response.
 * <p>
 * Allows access to modifying the response properties and registering actions to finalize the response and are executed before
 * sending the response.
 */
public interface ResponseMetaData {

  /**
   * The status that will be part of the response when sent.
   * <p>
   * By default, this will return a {@code "200 OK"} response.
   *
   * @return The status that will be part of the response when sent
   * @see #status
   */
  Status getStatus();

  /**
   * Sets the status line of the response.
   * <p>
   * The message used will be the standard for the code.
   *
   * @param code The status code of the response to use when it is sent.
   * @return This
   */
  ResponseMetaData status(int code);

  /**
   * Sets the status line of the response.
   *
   * @param status The status of the response to use when it is sent.
   * @return This
   */
  ResponseMetaData status(Status status);

  /**
   * The response headers.
   *
   * @return The response headers.
   */
  MutableHeaders getHeaders();

  /**
   * Sets the response {@code Content-Type} header.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @return This
   */
  ResponseMetaData contentType(CharSequence contentType);

  /**
   * Sets the response {@code Content-Type} header, if it has not already been set.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @return This
   */
  ResponseMetaData contentTypeIfNotSet(CharSequence contentType);

  /**
   * The cookies that are to be part of the response.
   * <p>
   * The cookies are mutable.
   *
   * @return The cookies that are to be part of the response.
   */
  Set<Cookie> getCookies();

  /**
   * Creates a new cookie with the given name and value.
   * <p>
   * The cookie will have no expiry. Use the returned cookie object to fine tune the cookie.
   *
   * @param name The name of the cookie
   * @param value The value of the cookie
   * @return The cookie that will be sent
   */
  Cookie cookie(String name, String value);

  /**
   * Adds a cookie to the response with a 0 max-age, forcing the client to expire it.
   *
   * @param name The name of the cookie to expire.
   * @return The created cookie
   */
  Cookie expireCookie(String name);

  ResponseMetaData noCompress();

}
