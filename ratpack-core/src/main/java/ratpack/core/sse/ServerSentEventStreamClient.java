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

package ratpack.core.sse;

import ratpack.core.http.client.HttpClient;
import ratpack.core.http.client.RequestSpec;
import ratpack.core.sse.internal.DefaultServerSentEventStreamClient;
import ratpack.exec.Promise;
import ratpack.exec.func.Action;
import ratpack.exec.stream.TransformablePublisher;

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

  Promise<TransformablePublisher<Event<?>>> request(URI uri, Action<? super RequestSpec> action);

  default Promise<TransformablePublisher<Event<?>>> request(URI uri) {
    return request(uri, Action.noop());
  }

}
