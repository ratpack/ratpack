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
import ratpack.func.Action;

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
public interface Response extends ResponseMetaData {

  @Override
  Response status(int code);

  @Override
  Response status(Status status);

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
  void send(CharSequence contentType, InputStream inputStream) throws IOException;

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

  @Override
  Response contentType(CharSequence contentType);

  @Override
  Response contentTypeIfNotSet(CharSequence contentType);

  /**
   * Sends the response, using the given content type and the content of the given type as the response body.
   * <p>
   * Prefer {@link #sendFile(java.nio.file.attribute.BasicFileAttributes, java.nio.file.Path)} where
   * the file attributes have already been retrieved to avoid another IO operation.
   *
   * @param file The file whose contents are to be used as the response body
   */
  @NonBlocking
  void sendFile(Path file);

  /**
   * Sends the response, using the given content type and the content of the given type as the response body.
   * @param attributes The attributes of the file, used for the headers
   * @param file The file whose contents are to be used as the response body
   */
  @NonBlocking
  void sendFile(BasicFileAttributes attributes, Path file);

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
   * If your data source produces small amount of data that is expensive to produce (i.e. there is a significant latency between a data request and the production of data)
   * you may want to consider an intermediate buffer to maximize throughput to the client.
   * However, this is rarely necessary.
   * <p>
   * The subscription to the publisher will occur via {@link ratpack.exec.ExecControl#stream(org.reactivestreams.Publisher, org.reactivestreams.Subscriber)}.
   *
   * @param stream a stream of byte bufs to be written to the response
   */
  @NonBlocking
  void sendStream(Publisher<? extends ByteBuf> stream);

  /**
   * Register an action to execute upon the response immediately before sending it to the client.
   *
   * @param responseFinalizer The action to execute on this response.
   * @return This
   */
  Response beforeSend(Action<? super ResponseMetaData> responseFinalizer);

}
