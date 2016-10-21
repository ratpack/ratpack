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

package ratpack.sse.internal;

import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.RequestSpec;
import ratpack.sse.Event;
import ratpack.sse.ServerSentEventStreamClient;
import ratpack.stream.TransformablePublisher;

import java.net.URI;

public class DefaultServerSentEventStreamClient implements ServerSentEventStreamClient {

  private final HttpClient httpClient;

  public DefaultServerSentEventStreamClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public Promise<TransformablePublisher<Event<?>>> request(URI uri, Action<? super RequestSpec> action) {
    return httpClient.requestStream(uri, action).map(r ->
      new ServerSentEventDecodingPublisher(r.getBody(), httpClient.getByteBufAllocator())
    );
  }

}
