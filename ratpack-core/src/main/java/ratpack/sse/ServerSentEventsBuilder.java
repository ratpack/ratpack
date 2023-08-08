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

package ratpack.sse;

import org.reactivestreams.Publisher;

import java.time.Duration;

/**
 * Creates a builder for a server sent event stream.
 *
 * @since 1.10
 */
public interface ServerSentEventsBuilder {

  /**
   * Specifies how events should be buffered.
   * <p>
   * By default, no buffering is applied.
   * This causes each event to be immediately written and flushed, which is generally inefficient.
   * Unless it is required to absolutely send all events as soon as they are available, buffering should be applied.
   * <p>
   * Events are buffered by number of events, number of bytes, and time.
   * The buffer will be flushed if any dimension is exceeded, and there is downstream demand.
   * <p>
   * Use {@link #buffered()} to use sensible defaults.
   *
   * @param numBytes the number of bytes to buffer (must be &gt; 0)
   * @param duration the amount of time to buffer events (use 0 to disable)
   * @return {@code this}
   * @see #buffered(int, Duration)
   */
  ServerSentEventsBuilder buffered(int numBytes, Duration duration);

  /**
   * Applies sensible buffering defaults, for low latency.
   * <ul>
   * <li>Bytes: 57344</li>
   * <li>Duration: 1 second</li>
   * </ul>
   *
   * @return {@code this}
   * @see #buffered(int, Duration)
   */
  default ServerSentEventsBuilder buffered() {
    return buffered(57344, Duration.ofSeconds(1));
  }

  /**
   * Causes a HTTP {@code 204 No Content} response to be rendered if the stream closes before sending any events.
   * <p>
   * When used in conjunction with {@link #buffered}, buffering begins after the first event is received.
   * As such, it is typically not suitable for long lived connections that stream data that will become available later.
   *
   * @return this
   */
  ServerSentEventsBuilder noContentOnEmpty();

  /**
   * Causes a comment to be written every {@code heartbeatAfterIdleFor} since the last write, to keep the connection alive.
   * <p>
   * By default, no keep alive is applied.
   *
   * @return this
   */
  ServerSentEventsBuilder keepAlive(Duration heartbeatAfterIdleFor);

  /**
   * Builds the server sent events with the given publisher.
   *
   * @param events the publisher of events
   * @return a renderable {@link ServerSentEvents}
   */
  ServerSentEvents build(Publisher<? extends ServerSentEvent> events);

}
