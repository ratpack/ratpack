/*
 * Copyright 2015 the original author or authors.
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

package ratpack.sse;

import io.netty.buffer.ByteBufAllocator;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.RequestSpec;
import ratpack.sse.internal.DefaultServerSentEventStreamClient;
import ratpack.stream.TransformablePublisher;
import ratpack.util.Exceptions;

import java.net.URI;

public interface ServerSentEventStreamClient {

  /**
   * Creates an SSE client.
   *
   * @param httpClient the underlying HTTP client to use
   * @return an SSE client
   * @since 1.4
   */
  static ServerSentEventStreamClient of(HttpClient httpClient) {
    return new DefaultServerSentEventStreamClient(httpClient);
  }

  /**
   * @deprecated since 1.4, use {@link #of(HttpClient)}.
   */
  @Deprecated
  static ServerSentEventStreamClient sseStreamClient(ByteBufAllocator byteBufAllocator) {
    return Exceptions.uncheck(() -> of(HttpClient.of(s -> s
      .poolSize(0)
      .byteBufAllocator(byteBufAllocator))
    ));
  }

  Promise<TransformablePublisher<Event<?>>> request(URI uri, Action<? super RequestSpec> action);

  default Promise<TransformablePublisher<Event<?>>> request(URI uri) {
    return request(uri, Action.noop());
  }

}
