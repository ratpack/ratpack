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

package ratpack.core.sse.internal;

import io.netty.buffer.ByteBuf;
import ratpack.core.sse.ServerSentEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ServerSentEventEncoder {

  private static final byte[] EVENT_PREFIX = utf8Bytes("event: ");

  private static final byte[] DATA_PREFIX_POOL = utf8Bytes("data: ");

  private static final byte[] ID_PREFIX = utf8Bytes("id: ");

  private static final byte[] COMMENT_PREFIX = utf8Bytes(": ");

  private static final byte NEWLINE = '\n';


  public static void encodeTo(ServerSentEvent sse, ByteBuf buffer) {
    writeMulti(buffer, COMMENT_PREFIX, sse.getComment());
    writeSingle(buffer, ID_PREFIX, sse.getId());
    writeSingle(buffer, EVENT_PREFIX, sse.getEvent());
    writeMulti(buffer, DATA_PREFIX_POOL, sse.getData());
    buffer.writeByte(NEWLINE);
  }

  private static void writeMulti(ByteBuf buffer, byte[] prefix, List<ByteBuf> comment) {
    comment.forEach(commentLine -> {
      buffer.writeBytes(prefix);
      buffer.writeBytes(commentLine);
      buffer.writeByte(NEWLINE);
    });
  }

  private static void writeSingle(ByteBuf buffer, byte[] prefix, ByteBuf element) {
    if (element.isReadable()) {
      buffer.writeBytes(prefix);
      buffer.writeBytes(element);
      buffer.writeByte(NEWLINE);
    }
  }

  private static byte[] utf8Bytes(String string) {
    return string.getBytes(StandardCharsets.UTF_8);
  }
}
