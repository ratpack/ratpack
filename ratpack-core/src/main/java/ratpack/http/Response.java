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
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.cookie.Cookie;
import org.reactivestreams.Publisher;
import ratpack.api.NonBlocking;
import ratpack.func.Action;
import ratpack.util.Types;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A response to a request.
 * <p>
 * The headers and status are configured, before committing the response with one of the {@link #send} methods.
 */
public interface Response {

  /**
   * A type token for this type.
   *
   * @since 1.1
   */
  TypeToken<Response> TYPE = Types.token(Response.class);

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
   * <p>
   * If the cookie that you want to expire has an explicit path, you must use {@link Cookie#setPath(String)} on the return
   * value of this method to have the cookie expire.
   *
   * @param name The name of the cookie to expire.
   * @return The created cookie
   */
  Cookie expireCookie(String name);

  /**
   * The cookies that are to be part of the response.
   * <p>
   * The cookies are mutable.
   *
   * @return The cookies that are to be part of the response.
   */
  Set<Cookie> getCookies();

  /**
   * The response headers.
   *
   * @return The response headers.
   */
  MutableHeaders getHeaders();

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
  default Response status(int code) {
    return status(Status.of(code));
  }

  /**
   * Sets the status line of the response.
   *
   * @param status The status of the response to use when it is sent.
   * @return This
   */
  Response status(Status status);

  /**
   * Sends the response back to the client, with no body.
   */
  @NonBlocking
  void send();

  Response contentTypeIfNotSet(Supplier<CharSequence> contentType);

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
   *  @param contentType The value of the content type header
   * @param body The string to render as the body of the response
   */
  @NonBlocking
  void send(CharSequence contentType, String body);

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
   *  @param contentType The value of the {@code Content-Type} header
   * @param bytes The response body
   */
  @NonBlocking
  void send(CharSequence contentType, byte[] bytes);

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
   *  @param contentType The value of the {@code Content-Type} header
   * @param buffer The response body
   */
  @NonBlocking
  void send(CharSequence contentType, ByteBuf buffer);

  /**
   * Sets the response {@code Content-Type} header.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @return This
   */
  Response contentType(CharSequence contentType);

  /**
   * Sets the response {@code Content-Type} header, if it has not already been set.
   *
   * @param contentType The value of the {@code Content-Type} header
   * @return This
   */
  default Response contentTypeIfNotSet(CharSequence contentType) {
    return contentTypeIfNotSet(() -> contentType);
  }

  /**
   * Sends the response, using the file as the response body.
   * <p>
   * This method does not set the content length, content type or anything else.
   * It is generally preferable to use the {@link ratpack.handling.Context#render(Object)} method with a file/path object,
   * or an {@link ratpack.handling.Chain#files(Action)}.
   *
   * @param file the response body
   */
  @NonBlocking
  void sendFile(Path file);

  /**
   * Sends the response, streaming the bytes emitted by the given publisher.
   * <p>
   * This method does not perform chunked transfer encoding.
   * It merely sends the raw bytes emitted by the publisher.
   * As such, it is generally preferable to {@link ResponseChunks <i>render</i> chunks} than use this method directly.
   * <p>
   * The response headers will be sent as is, without the implicit addition of a {@code Content-Length} header like the other send methods.
   * <p>
   * Back pressure is applied to the given publisher based on the flow control of the network connection.
   * That is, items are requested from the publisher as they are able to be sent by the underlying Netty layer.
   * As such, the given publisher <b>MUST</b> respect back pressure.
   * If this is not feasible, consider using {@link ratpack.stream.Streams#buffer(org.reactivestreams.Publisher)}.
   * <p>
   * The back pressure applied will be irregular, based on factors including:
   * <ul>
   * <li>Socket send/receive buffers</li>
   * <li>Client consumption rates</li>
   * <li>Size of the emitted byte buffers</li>
   * </ul>
   * <p>
   * Data requested of the publisher is not always written immediately to the client.
   * Netty maintains its own buffer that is fed by the given publisher.
   * This means that data is more likely ready to send as soon as the client receives it.
   * <p>
   * If your data source produces a small amount of data that is expensive to produce (i.e. there is a significant latency between a data request and the production of data)
   * you may want to consider an intermediate buffer to maximize throughput to the client.
   * However, this is rarely necessary.
   *
   * @param stream a stream of byte bufs to be written to the response
   */
  @NonBlocking
  void sendStream(Publisher<? extends ByteBuf> stream);

  /**
   * Register a callback to execute with the response immediately before sending it to the client.
   * <p>
   * This method is often used to add response headers “at the last second”.
   * <p>
   * The callbacks are executed after one of the {@code send*} methods.
   * As such, those methods cannot be called during an action given to this method.
   *
   * @param responseFinalizer the action to execute on the response.
   * @return {@code this}
   */
  Response beforeSend(@NonBlocking Action<? super Response> responseFinalizer);

  /**
   * Prevents the response from being compressed.
   *
   * @return {@code this}
   */
  Response noCompress();

  /**
   * Forces the closing of the current connection, even if the client requested it to be kept alive.
   * <p>
   * This method can be used when it is desirable to force the client's connection to close, defeating HTTP keep alive.
   * This can be desirable in some networking environments where rate limiting or throttling is performed via edge routers or similar.
   * <p>
   * This method simply calls {@code getHeaders().set("Connection", "close")}, which has the same effect.
   *
   * @return {@code this}
   * @since 1.1
   */
  default Response forceCloseConnection() {
    getHeaders().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    return this;
  }
}
