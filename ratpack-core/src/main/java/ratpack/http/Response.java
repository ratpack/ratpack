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
import org.reactivestreams.Publisher;
import ratpack.api.NonBlocking;
import ratpack.exec.ExecControl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

/**
 * A response to a request.
 * <p>
 * The headers and status are configured, before committing the response with one of the {@link #send} methods.
 */
@SuppressWarnings("UnusedDeclaration")
public interface Response {

  /**
   * The status that will be part of the response when sent.
   * <p>
   * By default, this will return a {@code "200 OK"} response.
   *
   * @return The status that will be part of the response when sent
   * @see #status
   */
  MutableStatus getStatus();

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
   * The response headers.
   *
   * @return The response headers.
   */
  MutableHeaders getHeaders();

  /**
   * Sends the response back to the client, with no body.
   */
  @NonBlocking
  public void send();

  /**
   * Sends the response, using "{@code text/plain}" as the content type and the given string as the response body.
   * <p>
   * Equivalent to calling "{@code send\("text/plain", text)}.
   *
   * @param text The text to render as a plain text response.
   */
  @NonBlocking
  void send(String text);

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
  @NonBlocking
  void send(String contentType, String body);

  /**
   * Sends the response, using "{@code application/octet-stream}" as the content type (if a content type hasn't
   * already been set) and the given byte array as the response body.
   *
   * @param bytes The response body
   */
  @NonBlocking
  void send(byte[] bytes);

  /**
   * Sends the response, using the given content type and byte array as the response body.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @param bytes The response body
   */
  @NonBlocking
  void send(String contentType, byte[] bytes);

  /**
   * Sends the response, using "{@code application/octet-stream}" as the content type (if a content type hasn't
   * already been set) and the contents of the given input stream as the response body.
   *
   * @param inputStream The response body
   * @throws IOException if the input stream cannot be consumed
   */
  @NonBlocking
  void send(InputStream inputStream) throws IOException;

  /**
   * Sends the response, using the given content type and the content of the given input stream as the response body.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @param inputStream response body
   * @throws IOException if the input stream cannot be consumed
   */
  @NonBlocking
  void send(String contentType, InputStream inputStream) throws IOException;

  /**
   * Sends the response, using "{@code application/octet-stream}" as the content type (if a content type hasn't
   * already been set) and the given bytes as the response body.
   *
   * @param buffer The response body
   */
  @NonBlocking
  void send(ByteBuf buffer);

  /**
   * Sends the response, using the given content type and bytes as the response body.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @param buffer The response body
   */
  @NonBlocking
  void send(String contentType, ByteBuf buffer);

  @NonBlocking
  void send(ExecControl execContext, Publisher<HttpResponseChunk> stream);

  /**
   * Sets the response {@code Content-Type} header.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @return This
   */
  Response contentType(String contentType);

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

  /**
   * Sends the response, using the given content type and the content of the given type as the response body.
   * <p>
   * Prefer {@link #sendFile(ratpack.exec.ExecControl, java.nio.file.attribute.BasicFileAttributes, java.nio.file.Path)} where
   * the file attributes have already been retrieved to avoid another IO operation.
   *
   * @param execContext the execution context to perform any blocking operations with
   * @param file The file whose contents are to be used as the response body
   */
  @NonBlocking
  void sendFile(ExecControl execContext, Path file) throws Exception;

  /**
   * Sends the response, using the given content type and the content of the given type as the response body.
   *
   * @param execContext the execution context to perform any blocking operations with
   * @param attributes The attributes of the file, used for the headers
   * @param file The file whose contents are to be used as the response body
   */
  @NonBlocking
  void sendFile(ExecControl execContext, BasicFileAttributes attributes, Path file) throws Exception;

  @NonBlocking
  void sendServerSentEventStream(ExecControl execContext, Publisher<ServerSentEvent> stream);
}
