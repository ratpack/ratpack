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

package ratpack.core.sse.client;

import ratpack.core.http.client.StreamedResponse;
import ratpack.core.sse.ServerSentEvent;
import ratpack.exec.stream.TransformablePublisher;

/**
 * A response for a server sent event stream.
 *
 * @since 1.10
 */
public interface ServerSentEventResponse extends StreamedResponse {

  /**
   * Whether the response is actually an event stream.
   * <p>
   * The server may have responded with some other type of content, or an error status code.
   * <p>
   * If this method returns false, {@link #getEvents()} will throw an exception when called.
   * The non event-stream response body can be read via {@link #getBody()}.
   *
   * @return whether the response is actually an event stream
   */
  boolean isEventStream();

  /**
   * The response body parsed as events.
   *
   * If {@link #isEventStream()} returns false, this method will throw an exception when called.
   * The non event-stream response body can be read via {@link #getBody()}.
   *
   * @return the event stream
   */
  TransformablePublisher<ServerSentEvent> getEvents();

}
