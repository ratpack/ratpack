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

package ratpack.file.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Subscriber;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.internal.ManagedSubscription;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicBoolean;


@SuppressWarnings("RedundantCast") // works around compiler bug
public class FileReadingPublisher implements TransformablePublisher<ByteBuf> {

  private final Promise<? extends AsynchronousFileChannel> file;
  private final int bufferSize;
  private final ByteBufAllocator allocator;
  private final long start;
  private final long stop;

  public FileReadingPublisher(Promise<? extends AsynchronousFileChannel> file, ByteBufAllocator allocator, int bufferSize, long start, long stop) {
    this.file = file;
    this.bufferSize = bufferSize;
    this.allocator = allocator;
    this.start = start;
    this.stop = stop < 1 ? Long.MAX_VALUE : stop;

    if (bufferSize < 0) {
      throw new IllegalArgumentException("bufferSize must be 0 or positive");
    }

    if (start < 0) {
      throw new IllegalArgumentException("start must be 0 or positive");
    }
  }

  @Override
  public void subscribe(Subscriber<? super ByteBuf> s) {
    file
      .onError(s::onError)
      .then(channel ->
        s.onSubscribe(new ManagedSubscription<ByteBuf>(s, ByteBuf::release) {

          private final AtomicBoolean reading = new AtomicBoolean();
          private long position = start;

          @Override
          protected void onRequest(long n) {
            doRead();
          }

          @Override
          protected void onCancel() {

          }

          private void read() {
            if (reading.compareAndSet(false, true)) {
              doRead();
            }
          }

          private void doRead() {
            Promise.<ByteBuf>async(down -> {
              int size = (int) Math.min(stop - position, bufferSize);
              ByteBuf buffer = allocator.buffer(size, size);
              channel.read(buffer.nioBuffer(0, size), position, buffer, new CompletionHandler<Integer, ByteBuf>() {
                @Override
                public void completed(Integer result, ByteBuf attachment) {
                  attachment.writerIndex(Math.max(result, 0));
                  down.success(attachment);
                }

                @Override
                public void failed(Throwable exc, ByteBuf attachment) {
                  attachment.release();
                  down.error(exc);
                }
              });
            })
              .onError(this::complete)
              .then(read -> {
                if (read.readableBytes() == 0) {
                  read.release();
                  complete(null);
                } else {
                  position += read.readableBytes();
                  emitNext(read);
                  if (position == stop) {
                    complete(null);
                  } else if (hasDemand()) {
                    doRead();
                  } else {
                    reading.set(false);
                    if (hasDemand()) {
                      read();
                    }
                  }
                }
              });
          }

          private void complete(Throwable error) {
            Promise<?> p = error == null ? Promise.ofNull() : Promise.error(error);
            p.close(Blocking.op(((AsynchronousFileChannel) channel)::close))
              .onError(this::emitError)
              .then(v -> emitComplete());
          }
        })
      );
  }

}
