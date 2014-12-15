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
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.stream.TransformablePublisher;

public interface StreamedResponse {
  /**
   *
   * @return {@link ratpack.http.Status} of the response.
   */
  Status getStatus();

  /**
   *
   * @return The integer status code of the response.
   */
  int getStatusCode();

  /**
   *
   * @return {@link ratpack.http.Headers} from the response.
   */
  Headers getHeaders();

  TransformablePublisher<ByteBuf> getBody();

  void send(Response response);

  void send(Response response, Action<? super MutableHeaders> headerMutator);
}
