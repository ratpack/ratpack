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

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.util.CharsetUtil;
import ratpack.sse.Event;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static io.netty.util.CharsetUtil.UTF_8;

public class ServerSentEventEncoder {

  public static final ServerSentEventEncoder INSTANCE = new ServerSentEventEncoder();

  private static final byte[] EVENT_TYPE_PREFIX = "event: ".getBytes(UTF_8);
  private static final byte[] EVENT_DATA_PREFIX = "data: ".getBytes(UTF_8);
  private static final byte[] EVENT_ID_PREFIX = "id: ".getBytes(UTF_8);
  private static final byte[] NEWLINE = "\n".getBytes(UTF_8);

  public ByteBuf encode(Event<?> event, ByteBufAllocator bufferAllocator) throws Exception {
    ByteBuf buffer = bufferAllocator.buffer();

    OutputStream outputStream = new ByteBufOutputStream(buffer);
    Writer writer = new OutputStreamWriter(outputStream, CharsetUtil.encoder(UTF_8));

    String eventId = event.getId();
    if (eventId != null) {
      outputStream.write(EVENT_ID_PREFIX);
      writer.append(eventId).flush();
      outputStream.write(NEWLINE);
    }

    String eventType = event.getEvent();
    if (eventType != null) {
      outputStream.write(EVENT_TYPE_PREFIX);
      writer.append(eventType).flush();
      outputStream.write(NEWLINE);
    }

    String eventData = event.getData();
    if (eventData != null) {
      outputStream.write(EVENT_DATA_PREFIX);
      for (Character character : Lists.charactersOf(eventData)) {
        if (character == '\n') {
          outputStream.write(NEWLINE);
          outputStream.write(EVENT_DATA_PREFIX);
        } else {
          writer.append(character).flush();
        }
      }
      outputStream.write(NEWLINE);
    }

    outputStream.write(NEWLINE);
    writer.flush();
    writer.close();
    return buffer;
  }
}
