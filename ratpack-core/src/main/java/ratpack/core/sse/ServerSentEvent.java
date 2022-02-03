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

package ratpack.core.sse;

import ratpack.func.Nullable;
import ratpack.core.sse.internal.DefaultServerSentEvent;

/**
 * A server sent event.
 *
 * @since 1.10
 */
public interface ServerSentEvent {

  /**
   * Creates a builder for an event.
   *
   * @return a builder for an event
   */
  static ServerSentEventBuilder builder() {
    return new DefaultServerSentEvent();
  }

  /**
   * The “id” value of the event.
   *
   * @return the “id” value of the event
   */
  @Nullable
  String getId();

  /**
   * The “event” value of the event.
   *
   * @return the “event” value of the event
   */
  @Nullable
  String getEvent();

  /**
   * The “data” value of the event.
   *
   * @return the “data” value of the event
   */
  @Nullable
  String getData();

  /**
   * The “comment” value of the event.
   *
   * @return the “comment” value of the event
   */
  @Nullable
  String getComment();

}
