/*
 * Copyright 2017 the original author or authors.
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

package ratpack.exec.stream.bytebuf.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import ratpack.exec.stream.TransformablePublisher;

import java.time.Duration;

public class ByteBufComposingPublisher implements TransformablePublisher<ByteBuf> {

  private final Publisher<? extends ByteBuf> upstream;
  private final ByteBufAllocator alloc;
  private final int maxNum;
  private final long watermark;

  public ByteBufComposingPublisher(int maxNum, long sizeWatermark, ByteBufAllocator alloc, Publisher<? extends ByteBuf> upstream) {
    this.upstream = upstream;
    this.alloc = alloc;
    this.maxNum = maxNum;
    this.watermark = sizeWatermark;
  }

  @Override
  public void subscribe(Subscriber<? super ByteBuf> downstream) {
    new ByteBufBufferingSubscription<ByteBuf>(upstream, ByteBuf::release, downstream, null, System::nanoTime, Duration.ZERO, Duration.ZERO, Unpooled.EMPTY_BUFFER) {
      private CompositeByteBuf buffer;

      protected boolean bufferIsFull() {
        return buffer != null && (buffer.numComponents() == maxNum || buffer.readableBytes() >= watermark);
      }

      @Override
      protected boolean isEmpty() {
        return buffer == null;
      }

      protected ByteBuf flush() {
        if (buffer == null) {
          throw new IllegalStateException();
        }
        CompositeByteBuf emittedBuffer = this.buffer;
        this.buffer = null; // unset before emitting in case emit causes cancel, which would cause double release
        return emittedBuffer;
      }

      @Override
      protected void buffer(ByteBuf item) {
        if (buffer == null) {
          buffer = alloc.compositeBuffer(maxNum);
        }
        buffer.addComponent(true, item);
      }

      @Override
      protected void discard() {
        ReferenceCountUtil.release(buffer);
        this.buffer = null;
      }
    }.connect();
  }

}
