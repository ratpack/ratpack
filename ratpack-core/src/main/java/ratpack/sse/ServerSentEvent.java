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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;
import ratpack.sse.internal.DefaultServerSentEvent;

import java.util.List;

import static ratpack.sse.internal.DefaultServerSentEvent.asString;

/**
 * A server sent event.
 * <p>
 * This object maintains references to several bytebufs.
 * Releasing this object releases each held buffer.
 *
 * @since 1.10
 */
public interface ServerSentEvent extends ReferenceCounted {

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
  ByteBuf getId();

  /**
   * The “id” value of the event.
   *
   * @return the “id” value of the event
   */
  default String getIdAsString() {
    return asString(getId());
  }

  /**
   * The “event” value of the event.
   *
   * @return the “event” value of the event
   */
  ByteBuf getEvent();

  /**
   * The “event” value of the event.
   *
   * @return the “event” value of the event
   */
  default String getEventAsString() {
    return asString(getEvent());
  }

  /**
   * The “data” value of the event.
   * <p>
   * Each list element corresponds to a line of data.
   *
   * @return the “data” value of the event
   */
  List<ByteBuf> getData();

  /**
   * The “data” value of the event.
   * <p>
   *
   * @return the “data” value of the event
   */
  default String getDataAsString() {
    return DefaultServerSentEvent.asMultilineString(getData());
  }

  /**
   * The “comment” value of the event.
   *
   * @return the “comment” value of the event
   */
  List<ByteBuf> getComment();

  /**
   * The “comment” value of the event.
   * <p>
   * Each list element corresponds to a line of data.
   *
   * @return the “comment” value of the event
   */
  default String getCommentAsString() {
    return DefaultServerSentEvent.asMultilineString(getComment());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  ServerSentEvent retain();

  /**
   * {@inheritDoc}
   */
  @Override
  ServerSentEvent retain(int increment);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerSentEvent touch();

  /**
   * {@inheritDoc}
   */
  @Override
  ServerSentEvent touch(Object hint);

  /**
   * Joins a list of byte buf lines to a single byte buf.
   * <p>
   * Can be used with {@link #getData()} or {@link #getComment()} to obtain the multi-line value.
   *
   * @param lines a list of UTF-8 lines
   * @return a new byte buffer of the lines joined with a newline
   */
  static ByteBuf join(List<ByteBuf> lines) {
    if (lines.isEmpty()) {
      return Unpooled.EMPTY_BUFFER;
    }
    if (lines.size() == 1) {
      return lines.get(0).retainedSlice();
    }

    int components = lines.size() * 2 - 1;
    ByteBuf[] byteBufs = new ByteBuf[components];
    byteBufs[0] = lines.get(0).retainedSlice();
    for (int i = 1; i < lines.size(); ++i) {
      byteBufs[i * 2 - 1] = DefaultServerSentEvent.NEWLINE_BYTE_BUF.slice();
      byteBufs[i * 2] = lines.get(i).retainedSlice();
    }

    return Unpooled.wrappedBuffer(byteBufs);
  }
}
