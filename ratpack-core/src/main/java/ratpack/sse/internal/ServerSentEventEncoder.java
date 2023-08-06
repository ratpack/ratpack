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
import io.netty.buffer.Unpooled;
import ratpack.sse.ServerSentEvent;

import java.util.List;

import static io.netty.util.CharsetUtil.UTF_8;

public class ServerSentEventEncoder {

  public static final ServerSentEventEncoder INSTANCE = new ServerSentEventEncoder();

  private static final ByteBuf EVENT_PREFIX = constant("event: ".getBytes(UTF_8));

  private static final ByteBuf DATA_PREFIX = constant("data: ".getBytes(UTF_8));
  private static final ByteBuf ID_PREFIX = constant("id: ".getBytes(UTF_8));
  private static final ByteBuf COMMENT_PREFIX = constant(": ".getBytes(UTF_8));

  private static final ByteBuf NEWLINE = DefaultServerSentEvent.NEWLINE_BYTE_BUF;

  public ByteBuf encode(ServerSentEvent event) throws Exception {
    return Unpooled.wrappedBuffer(
        component(COMMENT_PREFIX, event.getComment()),
        component(ID_PREFIX, event.getId()),
        component(EVENT_PREFIX, event.getEvent()),
        component(DATA_PREFIX, event.getData()),
        NEWLINE
    );
  }

  private static ByteBuf component(ByteBuf linePrefix, List<ByteBuf> lines) {
    if (lines.isEmpty()) {
      return Unpooled.EMPTY_BUFFER;
    }
    int size = lines.size();
    if (size == 1) {
      return component(linePrefix, lines.get(0));
    }

    ByteBuf[] parts = new ByteBuf[size * 3];
    for (int i = 0; i < size; ++i) {
      int j = i * 3;
      parts[j] = linePrefix.slice();
      parts[++j] = lines.get(i);
      parts[++j] = NEWLINE.slice();
    }

    return Unpooled.wrappedBuffer(parts);
  }

  private static ByteBuf component(ByteBuf prefix, ByteBuf byteBuf) {
    if (byteBuf.readableBytes() == 0) {
      return Unpooled.EMPTY_BUFFER;
    } else {
      return Unpooled.wrappedBuffer(prefix.slice(), byteBuf, NEWLINE.slice());
    }
  }


  private static ByteBuf constant(byte[] bytes) {
    return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(bytes).asReadOnly());
  }

}
