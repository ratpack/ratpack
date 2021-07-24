/*
 * Copyright 2021 the original author or authors.
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

package ratpack.sse.client;

import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.RequestSpec;
import ratpack.sse.client.internal.DefaultServerSentEventClient;

import java.net.URI;

/**
 * A client for request Server Sent Event streams.
 *
 * @since 1.10
 */
public interface ServerSentEventClient {

  /**
   * Creates a client that uses the given HTTP client.
   *
   * @param httpClient the HTTP client to use
   * @return a server sent event client
   */
  static ServerSentEventClient of(HttpClient httpClient) {
    return new DefaultServerSentEventClient(httpClient);
  }

  /**
   * Makes a request for an event stream to the given location.
   *
   * @param uri the location of the event stream
   * @param action the request configurer
   * @return the response
   */
  Promise<ServerSentEventResponse> request(URI uri, Action<? super RequestSpec> action);

  /**
   * Makes a request for an event stream to the given location.
   *
   * @param uri the location of the event stream
   * @return the response
   */
  default Promise<ServerSentEventResponse> request(URI uri) {
    return request(uri, Action.noop());
  }

}
