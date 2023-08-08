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

package ratpack.sse.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import ratpack.sse.ServerSentEvent;
import ratpack.sse.ServerSentEventBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultServerSentEvent implements ServerSentEvent, ServerSentEventBuilder {

  private static final byte NEWLINE_BYTE = (byte) '\n';
  public static final ByteBuf NEWLINE_BYTE_BUF = Unpooled.unreleasableBuffer(
      Unpooled.wrappedBuffer(new byte[]{NEWLINE_BYTE}).asReadOnly()
  );

  private ByteBuf id = Unpooled.EMPTY_BUFFER;
  private ByteBuf event = Unpooled.EMPTY_BUFFER;
  private List<ByteBuf> data = Collections.emptyList();

  private List<ByteBuf> comment = Collections.emptyList();

  @Override
  public ByteBuf getId() {
    return id;
  }

  @Override
  public ByteBuf getEvent() {
    return event;
  }

  @Override
  public List<ByteBuf> getData() {
    return data;
  }

  @Override
  public List<ByteBuf> getComment() {
    return comment;
  }

  @Override
  public ServerSentEventBuilder id(ByteBuf id) {
    if (containsNewline(id)) {
      throw new IllegalArgumentException("id must not contain \\n - '" + id + "'");
    }
    ReferenceCountUtil.release(this.id);
    this.id = id;
    return this;
  }

  @Override
  public ServerSentEventBuilder event(ByteBuf event) {
    if (containsNewline(event)) {
      throw new IllegalArgumentException("event must not contain \\n - '" + event + "'");
    }
    ReferenceCountUtil.release(this.event);
    this.event = event;
    return this;
  }

  @Override
  public ServerSentEventBuilder data(ByteBuf data) {
    return unsafeDataLines(toLines(data));
  }

  @Override
  public ServerSentEventBuilder unsafeDataLines(List<ByteBuf> data) {
    this.data.forEach(ReferenceCountUtil::release);
    this.data = data;
    return this;
  }

  @Override
  public ServerSentEventBuilder comment(ByteBuf comment) {
    return unsafeCommentLines(toLines(comment));
  }

  @Override
  public ServerSentEventBuilder unsafeCommentLines(List<ByteBuf> comment) {
    this.comment.forEach(ReferenceCountUtil::release);
    this.comment = comment;
    return this;

  }

  @Override
  public ServerSentEvent build() {
    return this;
  }

  private static boolean containsNewline(ByteBuf byteBuf) {
    return ByteBufUtil.indexOf(byteBuf, 0, byteBuf.readableBytes(), NEWLINE_BYTE) != -1;
  }

  public static String asString(ByteBuf byteBuf) {
    return byteBuf.toString(StandardCharsets.UTF_8);
  }

  public static String asMultilineString(List<ByteBuf> lines) {
    ByteBuf joined = ServerSentEvent.join(lines);
    String string = joined.toString(StandardCharsets.UTF_8);
    joined.release();
    return string;
  }

  public static List<ByteBuf> toLines(ByteBuf text) {
    try {
      int length = text.readableBytes();
      if (length == 0) {
        return Collections.emptyList();
      }

      int newlineIndex = ByteBufUtil.indexOf(text, 0, length, NEWLINE_BYTE);
      if (newlineIndex == -1) {
        return Collections.singletonList(text.retainedSlice());
      }

      List<ByteBuf> lines = new ArrayList<>();
      lines.add(text.retainedSlice(0, newlineIndex));
      int cursor = newlineIndex + 1;
      while (cursor <= length) {
        newlineIndex = ByteBufUtil.indexOf(text, cursor, length, NEWLINE_BYTE);
        if (newlineIndex == -1) {
          newlineIndex = length;
        }
        lines.add(text.retainedSlice(cursor, newlineIndex - cursor));
        cursor = newlineIndex + 1;
      }

      return lines;
    } finally {
      text.release();
    }
  }

  @Override
  public void close() {
    ReferenceCountUtil.safeRelease(id);
    ReferenceCountUtil.safeRelease(event);
    data.forEach(ReferenceCountUtil::safeRelease);
    comment.forEach(ReferenceCountUtil::safeRelease);
  }

}
