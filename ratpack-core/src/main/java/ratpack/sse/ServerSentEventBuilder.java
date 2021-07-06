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

import ratpack.api.Nullable;

/**
 * A server sent event builder.
 *
 * @since 1.10
 */
public interface ServerSentEventBuilder {

  /**
   * Specify the event id for the server sent event.
   * <p>
   * The value must not contain a {@code '\n'} character as this is not valid in an event value.
   *
   * @param id the event id
   * @return this
   */
  ServerSentEventBuilder id(@Nullable String id);

  /**
   * Specify the event type for the server sent event.
   * <p>
   * The value must not contain a {@code '\n'} character as this is not valid in an event value.
   *
   * @param event the event type
   * @return this
   */
  ServerSentEventBuilder event(@Nullable String event);

  /**
   * Specify the event data for the server sent event.
   *
   * @param data the event data
   * @return this
   */
  ServerSentEventBuilder data(@Nullable String data);

  /**
   * Specify a comment to include as part of this event.
   *
   * @param comment the comment data
   * @return this
   */
  ServerSentEventBuilder comment(@Nullable String comment);

  /**
   * Builds the event.
   *
   * @return the built event
   */
  ServerSentEvent build();

}
