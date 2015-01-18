/*
 * Copyright 2015 the original author or authors.
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
import ratpack.func.Function;
import ratpack.sse.Event;
import ratpack.stream.WriteStream;

public class ServerSentEventStreamMapDecoder implements Function<WriteStream<Event<?>>, WriteStream<ByteBuf>> {

  private final ByteBufAllocator bufferAllocator;

  public ServerSentEventStreamMapDecoder(ByteBufAllocator bufferAllocator) {
    this.bufferAllocator = bufferAllocator;
  }

  @Override
  public WriteStream<ByteBuf> apply(WriteStream<Event<?>> sseWriteStream) throws Exception {
    return new WriteStream<ByteBuf>() {
      @Override
      public void item(ByteBuf item) {
        try {
          ServerSentEventDecoder.INSTANCE.decode(item, bufferAllocator, sseWriteStream::item);
        } catch (Exception e) {
          error(e);
        }
      }

      @Override
      public void error(Throwable throwable) {
        sseWriteStream.error(throwable);
      }

      @Override
      public void complete() {
        sseWriteStream.complete();
      }
    };
  }
}
