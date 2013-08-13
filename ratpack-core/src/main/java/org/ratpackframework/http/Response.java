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

package org.ratpackframework.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import org.ratpackframework.api.Nullable;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * A response to a request.
 * <p>
 * The headers and status are configured, before committing the response with one of the {@link #send} methods.
 */
@SuppressWarnings("UnusedDeclaration")
public interface Response {

  /**
   * A status line of a HTTP response.
   */
  interface Status {
    /**
     * The status code.
     *
     * @return The status code.
     */
    int getCode();

    /**
     * The message of the status.
     *
     * @return The message of the status.
     */
    String getMessage();
  }

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
  Response status(int code);

  /**
   * Sets the status line of the response.
   *
   * @param code The status code of the response to use when it is sent.
   * @param message The status message of the response to use when it is sent.
   * @return This
   */
  Response status(int code, String message);

  /**
   * Sends the response back to the client, with no body.
   */
  public void send();

  /**
   * Sends the response, using the given content type and string as the response body.
   * <p>
   * The string will be sent in "utf8" encoding, and the given content type will have this appended.
   * That is, given a {@code contentType} of "{@code application/json}" the actual value for the {@code Content-Type}
   * header will be "{@code application/json;charset=utf8}".
   * <p>
   * The value given for content type will override any previously set value for this header.
   *
   * @param contentType The value of the content type header
   * @param body The string to render as the body of the response
   */
  void send(String contentType, String body);

  /**
   * Sends the response, using "{@code text/plain}" as the content type and the given string as the response body.
   * <p>
   * Equivalent to calling "{@code send\("text/plain", text)}.
   *
   * @param text The text to render as a plain text response.
   */
  void send(String text);

  /**
   * Sends the response, using the given content type and bytes as the response body.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @param buffer The response body
   */
  void send(String contentType, ByteBuf buffer);

  /**
   * Sends the response, using the given content type and the content of the given type as the response body.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @param file The file whose contents are to be used as the response body
   */
  void sendFile(String contentType, File file);

  /**
   * Sets the response {@code Content-Type} header.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @return This
   */
  Response contentType(String contentType);

  /**
   * Returns the header value with the specified header name.
   * <p>
   * If there is more than one header value for the specified header name, the first value is returned.
   *
   * @param name The case insensitive name of the header to get retrieve the first value of
   * @return the header value or {@code null} if there is no such header
   */
  @Nullable
  String getHeader(String name);

  /**
   * Returns all of the header values with the specified header name.
   *
   * @param name The case insensitive name of the header to retrieve all of the values of
   * @return the {@link List} of header values, or an empty list if there is no such header
   */
  List<String> getHeaders(String name);

  /**
   * Checks whether a header has been specified for the given value.
   *
   * @param name The name of the header to check the existence of
   * @return True if there is a header with the specified header name
   */
  boolean containsHeader(String name);

  /**
   * All header names.
   *
   * @return The names of all headers that will be sent
   */
  Set<String> getHeaderNames();

  /**
   * Adds a new header with the specified name and value.
   * <p>
   * Will not replace any existing values for the header.
   *
   * @param name The name of the header
   * @param value The value of the header
   */
  void addHeader(String name, Object value);

  /**
   * Sets the (only) value for the header with the specified name.
   * <p>
   * All existing values for the same header will be removed.
   *
   * @param name The name of the header
   * @param value The value of the header
   */
  void setHeader(String name, Object value);

  /**
   * Sets a new header with the specified name and values.
   * <p>
   * All existing values for the same header will be removed.
   *
   * @param name The name of the header
   * @param values The values of the header
   */
  void setHeader(String name, Iterable<?> values);

  /**
   * Removes the header with the specified name.
   *
   * @param name The name of the header to remove.
   */
  void removeHeader(String name);

  /**
   * Removes all headers from this message.
   */
  void clearHeaders();

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
}
