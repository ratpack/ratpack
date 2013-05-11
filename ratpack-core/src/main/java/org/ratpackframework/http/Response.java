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

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * A response to a request.
 */
@SuppressWarnings("UnusedDeclaration")
public interface Response {

  interface Status {
    int getCode();
    String getMessage();
  }

  Status getStatus();

  Response status(int status);

  Response status(int status, String message);

  /**
   * Finalize the request.
   */
  public void send();

  /**
   * Renders the toString() of the given object as the given content type.
   *
   * Sets the content type header to "$contentType;charset=utf-8" and finalizes the response.
   */
  void send(String contentType, String str);

  void send(String text);

  /**
   * Finalises the response, writing the buffer asynchronously
   *
   * @param buffer The response body
   */
  void send(String contentType, ByteBuf buffer);

  void sendFile(String contentType, File file);

  /**
   * Sends a temporary redirect response (i.e. statusCode 302) to the client using the specified redirect location URL.
   *
   * @param location the redirect location URL
   */
  void redirect(String location);

  /**
   * Sends a redirect response location URL and status code (which should be in the 3xx range).
   *
   * @param location the redirect location URL
   */
  void redirect(int code, String location);

  /**
   * Returns the header value with the specified header name.  If there are more than one header value for the specified header name, the first value is returned.
   *
   * @return the header value or {@code null} if there is no such header
   */
  String getHeader(String name);

  /**
   * Returns the header values with the specified header name.
   *
   * @return the {@link List} of header values.  An empty list if there is no such header.
   */
  List<String> getHeaders(String name);

  /**
   * Returns {@code true} if and only if there is a header with the specified header name.
   */
  boolean containsHeader(String name);

  /**
   * Returns the {@link java.util.Set} of all header names that this message contains.
   */
  Set<String> getHeaderNames();

  /**
   * Adds a new header with the specified name and value.
   */
  void addHeader(String name, Object value);

  /**
   * Sets a new header with the specified name and value.  If there is an existing header with the same name, the existing header is removed.
   */
  void setHeader(String name, Object value);

  /**
   * Sets a new header with the specified name and values.  If there is an existing header with the same name, the existing header is removed.
   */
  void setHeader(String name, Iterable<?> values);

  /**
   * Removes the header with the specified name.
   */
  void removeHeader(String name);

  /**
   * Removes all headers from this message.
   */
  void clearHeaders();

  /**
   * Returns any cookies that are scheduled to be sent with this response.
   */
  Set<Cookie> getCookies();

  /**
   * Convenience method for adding a cookie.
   */
  Cookie cookie(String name, String value);

  Cookie expireCookie(String name);
}
