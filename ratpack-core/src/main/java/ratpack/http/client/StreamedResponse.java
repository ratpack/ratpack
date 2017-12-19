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
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.stream.TransformablePublisher;

/**
 * A received response to a http client request with streamed response content.
 * <p>
 * The HTTP status and response headers are available immediately, the response content can be accessed by
 * subscribing to the {@link org.reactivestreams.Publisher} returned from {@link #getBody()} or can
 * be directly streamed as a server response using {@link #forwardTo(ratpack.http.Response, ratpack.func.Action)}.
 */
public interface StreamedResponse extends HttpResponse {

  /**
   *
   * @return a {@link org.reactivestreams.Publisher} of response content chunks.
   */
  TransformablePublisher<ByteBuf> getBody();

  /**
   * Stream this received response out to the given server response.
   *
   * @param response the server response to stream to
   * @see #forwardTo(ratpack.http.Response, ratpack.func.Action)
   */
  void forwardTo(Response response);

  /**
   * Stream this received response out to the given server response.
   * <p>
   * The HTTP status and response headers of this response will be copied to the given server response.  If this response
   * has a {@code content-length} http header it will be excluded from the copy as all responses will be streamed with a
   * {@code transfer-encoding} of {@code chunked}.  Outgoing response headers can be added and modified with the given
   * header mutating {@link ratpack.func.Action}.
   * <p>
   * This method will stream the response content chunks unmodified to the given server response using the publisher returned
   * from {@link #getBody()}.
   *
   * @param response the server response to stream to
   * @param headerMutator an action that will act on the outgoing response headers
   */
  void forwardTo(Response response, Action<? super MutableHeaders> headerMutator);
}
