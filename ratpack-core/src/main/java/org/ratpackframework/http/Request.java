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

import io.netty.handler.codec.http.Cookie;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A request to be handled.
 */
public interface Request {

  HttpMethod getMethod();

  String getUri();

  String getQuery();

  String getPath();

  Map<String, List<String>> getQueryParams();

  MediaType getContentType();

  Map<String, List<String>> getForm();

  Set<Cookie> getCookies();

  /**
   * Assumes that the user agent sent 0 or 1 cookies with the given name, returns it's value.
   *
   * If there is more than one cookie with this name, this method will throw an exception.
   *
   * @param name The name of the cookie to maybeGet the value of
   * @return The cookie value, or null if not present
   */
  String oneCookie(String name);

  String getText();

  /**
   * Returns the header value with the specified header name.  If there are more than one header value for the specified header name, the first value is returned.
   *
   * @return the header value or {@code null} if there is no such header
   */
  String getHeader(String name);

  Date getDateHeader(String name);

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

}
