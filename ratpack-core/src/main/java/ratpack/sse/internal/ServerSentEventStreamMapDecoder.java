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
import org.reactivestreams.Subscription;
import ratpack.sse.Event;
import ratpack.stream.StreamMapper;
import ratpack.stream.WriteStream;

public class ServerSentEventStreamMapDecoder implements StreamMapper<ByteBuf, Event<?>> {

  private final ByteBufAllocator bufferAllocator;

  public ServerSentEventStreamMapDecoder(ByteBufAllocator bufferAllocator) {
    this.bufferAllocator = bufferAllocator;
  }

  @Override
  public WriteStream<ByteBuf> map(Subscription subscription, WriteStream<Event<?>> down) throws Exception {
    return down.itemMap(subscription, item -> ServerSentEventDecoder.INSTANCE.decode(item, bufferAllocator, down::item));
  }
}
