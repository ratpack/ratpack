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
   *
   * @param maxRedirects Sets the maximum number of redirects to follow
   * @return  The RequestSpec
   */
  RequestSpec redirects(int maxRedirects);

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
   * <p>
   * It is generally best to provide the body content in the format that you have it in.
   * That is, if you already have the desired body content as a {@link String}, use the {@link #text(CharSequence)} method.
   * If you already have the desired body content as a {@code byte[]}, use the {@link #bytes(byte[])} method.
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
     * Specifies the request body as a byte buffer.
     * <p>
     * The given byte buffer will not be copied.
     * That is, changes to the byte buffer made after calling this method will affect the body.
     *
     * @param byteBuf the intended request body
     * @return this
     */
    Body buffer(ByteBuf byteBuf);

    /**
     * Specifies the request body as a byte array.
     * <p>
     * The given byte array will not be copied.
     * That is, changes to the byte array made after calling this method will affect the body.
     *
     * @param bytes the intended request body
     * @return this
     */
    Body bytes(byte[] bytes);

    /**
     * Specifies the request body as a UTF-8 char sequence.
     * <p>
     * This method is a shorthand for calling {@link #text(CharSequence, java.nio.charset.Charset)} with a UTF-8 charset.
     *
     * @param text the request body
     * @return this
     * @see #text(CharSequence, java.nio.charset.Charset)
     */
    Body text(CharSequence text);

    /**
     * Specifies the request body as a char sequence of the given charset.
     * <p>
     * Unlike other methods of this interface, this method will set the request {@code "Content-Type"} header if it has not already been set.
     * If it has not been set, it will be set to {@code "text/plain;charset=«charset»"}.
     *
     * @param text the request body
     * @param charset the charset of the request body (used to convert the text to bytes)
     * @return this
     */
    Body text(CharSequence text, Charset charset);

  }

}
