/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.app;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.Cookie;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A response to a request.
 */
@SuppressWarnings("UnusedDeclaration")
public interface Response {

  /**
   * The request that this response is for.
   *
   * @return the request that this response is for.
   */
  Request getRequest();

  /**
   * The status code that will be sent.
   *
   * Defaults to 200. Will be sent when the response is finalised.
   *
   * @return the status code that will be sent.
   */
  int getStatus();

  /**
   * The status code that will be sent.
   *
   * @param status the status code to send.
   */
  void setStatus(int status);

  /**
   * Render the template at the given path (relative to the configure templates dir) with the given model.
   *
   * Template rendering semantics are defined by the registered {@link org.ratpackframework.templating.TemplateRenderer}.
   *
   * This will finalize the response. Note that rendering may happen asynchronously. That is, these method may return immediately.
   *
   * @param model the model data.
   * @param templatePath the relative path to the template.
   */
  void render(Map<String, ?> model, String templatePath);

  /**
   * Calls {@link #render(java.util.Map, String)} with an empty map for the model.
   *
   * @param templatePath the relative path to the template.
   */
  void render(String templatePath);

  /**
   * Renders the toString() of the given object as plain text.
   *
   * If the content type has not been set, sets it to “text/plain;charset=utf-8” Finalizes the response.
   */
  void text(Object str);

  /**
   * Renders the toString() of the given object as the given content type.
   *
   * Sets the content type header to “$contentType;charset=utf-8” and finalizes the response.
   */
  void text(String contentType, Object str);

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
   * End the response with an error (500), by passing the exception to the error handler.
   *
   * @param error What went wrong
   */
  public void error(Exception error);

  /**
   * Finalize the request.
   */
  public void end();

  /**
   * Finalize the request, with the given status code.
   *
   * @param status The HTTP status code to respond with.
   */
  public void end(int status);

  /**
   * Finalize the request, with the given status code and message
   *
   * @param status The HTTP status code to respond with.
   * @param message The message to send back to the client as part of the status response
   */
  public void end(int status, String message);

  /**
   * Finalises the response, writing the buffer asynchronously
   *
   * @param buffer The response body
   */
  void end(ChannelBuffer buffer);

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
   * Returns the all header names and values that this message contains.
   *
   * @return the {@link List} of the header name-value pairs.  An empty list if there is no header in this message.
   */
  List<Map.Entry<String, String>> getHeaders();

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
