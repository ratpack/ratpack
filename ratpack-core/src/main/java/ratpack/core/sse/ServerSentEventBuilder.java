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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A server sent event builder.
 * <p>
 * The builder maintains references to provided byte bufs.
 * It is important to always {@link #build()} the event and ultimately ensure that the returned event is released.
 *
 * @since 1.10
 */
public interface ServerSentEventBuilder {

  /**
   * Specify the event ID for the server sent event.
   * <p>
   * The value must be a UTF-8 string that does not contain any newline characters.
   *
   * @param id the event ID
   * @return this
   */
  ServerSentEventBuilder id(ByteBuf id);

  /**
   * Specify the event ID for the server sent event.
   * <p>
   * The value must not contain any newline characters.
   *
   * @param id the event ID
   * @return this
   */
  default ServerSentEventBuilder id(String id) {
    return id(Unpooled.wrappedBuffer(id.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Specify the event type for the server sent event.
   * <p>
   * The value must be a UTF-8 string that does not contain any newline characters.
   *
   * @param event the event type
   * @return this
   */
  ServerSentEventBuilder event(ByteBuf event);

  /**
   * Specify the event type for the server sent event.
   * <p>
   * The value must not contain any newline characters.
   *
   * @param event the event type
   * @return this
   */
  default ServerSentEventBuilder event(String event) {
    return event(Unpooled.wrappedBuffer(event.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Specify the event data for the server sent event.
   * <p>
   * The value must be a UTF-8 string.
   *
   * @param data the event data
   * @return this
   */
  ServerSentEventBuilder data(ByteBuf data);

  /**
   * Specify the event data for the server sent event.
   * <p>
   * The value must be a UTF-8 string.
   *
   * @param data the event data
   * @return this
   */
  default ServerSentEventBuilder data(String data) {
    return data(Unpooled.wrappedBuffer(data.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Specify the event data for the server sent event.
   * <p>
   * The value must be a list of UTF-8 strings where no value contains a newline.
   * This value is not validated.
   * If any list element contains a newline character, the server sent event stream will be malformed.
   *
   * @param data the event data
   * @return this
   */
  ServerSentEventBuilder unsafeDataLines(List<ByteBuf> data);

  /**
   * Specify a comment to include as part of this event.
   *
   * @param comment the comment data
   * @return this
   */
  ServerSentEventBuilder comment(ByteBuf comment);

  /**
   * Specify a comment to include as part of this event.
   *
   * @param comment the comment data
   * @return this
   */
  default ServerSentEventBuilder comment(String comment) {
    return comment(Unpooled.wrappedBuffer(comment.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Specify a comment to include as part of this event.
   * <p>
   * The value must be a list of UTF-8 strings where no value contains a newline.
   * This value is not validated.
   * If any list element contains a newline character, the server sent event stream will be malformed.
   *
   * @param comment the comment data
   * @return this
   */
  ServerSentEventBuilder unsafeCommentLines(List<ByteBuf> comment);

  /**
   * Builds the event.
   *
   * @return the built event
   */
  ServerSentEvent build();

}
