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
   *
   * @return The {@link ratpack.http.client.RequestSpec.Body} of the request.
   */
  Body getBody();

  /**
   *
   * @param action Provide an action that will act on a Body
   * @return The RequestSpec
   * @throws Exception This can be thrown from the action supplied.
   */
  RequestSpec body(Action<? super Body> action) throws Exception;

  interface Body {
    /**
     * Convenience method of setting the Content-Type header.
     * @param contentType The value to set teh Content-Type header to.
     * @return This Body
     */
    Body type(String contentType);

    /**
     * One of the ways of providing body data. Should not be used in combination with the other methods, use of these methods negates the other uses.
     *
     * @param action Provide an action that will act on an OutputStream, this stream will be sent as the body.
     * @return This Body
     * @throws Exception Exception can be thrown from the action supplied.
     */
    Body stream(Action<? super OutputStream> action) throws Exception;

    /**
     * One of the ways of providing body data. Should not be used in combination with the other methods, use of these methods negates the other uses.
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

  }

}
