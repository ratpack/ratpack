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

package ratpack.stream.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.reactivestreams.Publisher;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.bytebuf.internal.ByteBufComposingPublisher;
import ratpack.util.Exceptions;

import java.io.BufferedInputStream;

/**
 * Utilities for dealing with streams of {@link ByteBuf}.
 *
 * @since 1.5
 */
public class ByteBufStreams {

  private ByteBufStreams() {
  }

  /**
   * Buffers and composes byte bufs together into composites before emitting.
   * <p>
   * This is roughly analogous to {@link BufferedInputStream}.
   * The returned published accumulates upstream buffers until {@code maxNum} have been received,
   * or the cumulative size of buffered byte bufs is greater than or equal to {@code sizeWatermark}.
   * Note that unlike {@link BufferedInputStream}, the downstream writes are not guaranteed to be less than the buffer size.
   * <p>
   * Byte bufs are requested of the given publisher one at a time.
   * If this is inefficient, consider wrapping it with {@link Streams#batch(int, Publisher, Action)} before giving to this method.
   *
   * @param publisher the publisher of byte bufs to compose
   * @param sizeWatermark the watermark size for a composite
   * @param maxNum the maximum number of composite components
   * @param alloc the allocator of composites
   * @return a byte buf composing publisher
   */
  public static TransformablePublisher<CompositeByteBuf> compose(Publisher<? extends ByteBuf> publisher, long sizeWatermark, int maxNum, ByteBufAllocator alloc) {
    return new ByteBufComposingPublisher(maxNum, sizeWatermark, alloc, publisher);
  }

  /**
   * Reduces the stream to a single composite byte buf.
   *
   * @param publisher the stream
   * @param alloc the buffer allocator
   * @return the reduced composite buffer
   */
  public static Promise<CompositeByteBuf> reduce(Publisher<? extends ByteBuf> publisher, ByteBufAllocator alloc) {
    return Promise.flatten(() -> {
      CompositeByteBuf seed = alloc.compositeBuffer();
      return Streams.reduce(publisher, seed, (c, b) -> c.addComponent(true, b))
        .onError(e -> {
          seed.release();
          throw Exceptions.toException(e);
        });
    });
  }
}
