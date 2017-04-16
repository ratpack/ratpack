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

package ratpack.sse.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ratpack.sse.Event;

import java.nio.charset.StandardCharsets;

import static io.netty.util.CharsetUtil.UTF_8;

public class ServerSentEventEncoder {

  public static final ServerSentEventEncoder INSTANCE = new ServerSentEventEncoder();

  private static final byte[] EVENT_TYPE_PREFIX = "event: ".getBytes(UTF_8);
  private static final byte[] EVENT_DATA_PREFIX = "data: ".getBytes(UTF_8);
  private static final byte[] EVENT_ID_PREFIX = "id: ".getBytes(UTF_8);
  private static final byte NEWLINE = '\n';

  public ByteBuf encode(Event<?> event, ByteBufAllocator bufferAllocator) throws Exception {
    String eventId = event.getId();
    String eventType = event.getEvent();
    String eventData = event.getData();

    int initialCapacity = eventData == null ? 8192 : eventData.length() + 8192;
    ByteBuf buffer = bufferAllocator.buffer(initialCapacity);

    if (eventId != null) {
      buffer.writeBytes(EVENT_ID_PREFIX);
      buffer.writeCharSequence(eventId, StandardCharsets.UTF_8);
      buffer.writeByte(NEWLINE);
    }

    if (eventType != null) {
      buffer.writeBytes(EVENT_TYPE_PREFIX);
      buffer.writeCharSequence(eventType, StandardCharsets.UTF_8);
      buffer.writeByte(NEWLINE);
    }

    int from = 0;
    if (eventData != null) {
      int length = eventData.length();
      buffer.writeBytes(EVENT_DATA_PREFIX);
      while (from < length) {
        int to = eventData.indexOf('\n', from);
        if (to == -1) {
          buffer.writeCharSequence(eventData.substring(from), StandardCharsets.UTF_8);
          break;
        } else {
          buffer.writeCharSequence(eventData.substring(from, to), StandardCharsets.UTF_8);
          buffer.writeByte(NEWLINE);
          buffer.writeBytes(EVENT_DATA_PREFIX);
          from = to + 1;
        }
      }
      buffer.writeByte(NEWLINE);
    }

    buffer.writeByte(NEWLINE);

    return buffer;
  }
}
