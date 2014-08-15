/*
 * Copyright 2014 the original author or authors.
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

package ratpack.http.client;

import io.netty.buffer.ByteBuf;
import ratpack.func.Action;
import ratpack.http.HttpUrlSpec;
import ratpack.http.MutableHeaders;

import java.io.OutputStream;
import java.nio.charset.Charset;

public interface RequestSpec {

  /**
   * @return {@link ratpack.http.MutableHeaders} that can be used to configure the headers that will be used for the request.
   */
  MutableHeaders getHeaders();

  /**
   * This method can be used to compose changes to the headers.
   *
   * @param action Provide an action that will act on MutableHeaders.
   * @return The RequestSpec
   * @throws Exception This can be thrown from the action supplied.
   */
  RequestSpec headers(Action<? super MutableHeaders> action) throws Exception;

  /**
   * Set the HTTP verb to use.
   * @param method which HTTP verb to use
   * @return this
   */
  RequestSpec method(String method);

  HttpUrlSpec getUrl();

  RequestSpec url(Action<? super HttpUrlSpec> action) throws Exception;

  /**
   * The body of the request, used for specifying the body content.
   *
   * @return the (writable) body of the request
   */
  Body getBody();

  /**
   * Executes the given action with the {@link #getBody() request body}.
   * <p>
   * This method is a “fluent API” alternative to {@link #getBody()}.
   *
   * @param action configuration of the request body
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  RequestSpec body(Action<? super Body> action) throws Exception;

  /**
   * The request body.
   * <p>
   * The methods of this type are not additive.
   * That is, successive calls to {@link #bytes(byte[])} and other methods will not add to the request body.
   * Rather, they will replace the content specified by previous method calls.
   */
  interface Body {

    /**
     * Specifies the {@code "Content-Type"} of the request.
     * <p>
     * Call this method has the same effect as using {@link #getHeaders()} or {@link #headers(ratpack.func.Action)} to set the {@code "Content-Type"} header.
     *
     * @param contentType the value of the Content-Type header
     * @return this
     */
    Body type(String contentType);

    /**
     * Specifies the request body by writing to an output stream.
     * <p>
     * The output stream is not directly connected to the HTTP server.
     * That is, bytes written to the given output stream are not directly streamed to the server.
     * There is no performance advantage in using this method over methods such as {@link #bytes(byte[])}.
     *
     * @param action an action that writes to the request body to the
     * @return this
     * @throws Exception any thrown by action
     */
    Body stream(Action<? super OutputStream> action) throws Exception;

    /**
     * Specifies the request body
     *
     * @param byteBuf Provide a ByteBuf that will be sent as the body of the request.
     * @return This Body
     */
    Body buffer(ByteBuf byteBuf);

    /**
     * One of the ways of providing body data. Should not be used in combination with the other methods, use of these methods negates the other uses.
     *
     * @param bytes Provide an array of bytes to be sent as the body of the request.
     * @return This Body
     */
    Body bytes(byte[] bytes);

    Body text(CharSequence text);

    Body text(CharSequence text, Charset charset);

  }

}
