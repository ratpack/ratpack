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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import ratpack.api.Nullable;
import ratpack.util.MultiValueMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * A request to be handled.
 */
@SuppressWarnings("UnusedDeclaration")
public interface Request {

  /**
   * The method of the request.
   *
   * @return The method of the request.
   */
  HttpMethod getMethod();

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

  ByteBuf getBuffer();

  /**
   * TBD.
   *
   * @return TBD.
   */
  MultiValueMap<String, String> getForm();

  /**
   * A structured representation of the "Content-Type" header value of the request.
   *
   * @return A structured representation of the "Content-Type" header value of the request.
   */
  MediaType getContentType();

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
   * The request body as text.
   * <p>
   * The encoding used will be determined by the value of the Content-Type header of the request.
   *
   * @return The request body as text, or an empty string if the request has no body.
   */
  String getText();

  /**
   * The request body as bytes.
   * <p>
   * If there is no request body, or it is 0 length, an empty byte array will be returned.
   *
   * @return The request body as bytes.
   */
  byte[] getBytes();

  /**
   * Writes the request body bytes to the given output stream.
   * <p>
   * If there is no request body, or it is 0 length, nothing will be written to the stream.
   *
   * @param destination The stream to write the bytes to
   * @throws IOException If destination throws an exception during writing
   */
  void writeBodyTo(OutputStream destination) throws IOException;

  /**
   * Provides an input stream of the request body.
   *
   * @return an input stream of the request body.
   */
  InputStream getInputStream();

  /**
   * The request headers.
   *
   * @return The request headers.
   */
  Headers getHeaders();
}
