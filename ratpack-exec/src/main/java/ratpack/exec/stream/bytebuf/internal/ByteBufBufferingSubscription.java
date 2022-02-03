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

package ratpack.exec.stream.bytebuf.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.stream.internal.ManagedSubscription;

public class ByteBufBufferingSubscription extends ManagedSubscription<ByteBuf> {

  private final Publisher<? extends ByteBuf> upstream;
  private final ByteBufAllocator alloc;
  private final int maxNum;
  private final long watermark;

  private Subscription subscription;
  private CompositeByteBuf buffer;

  public ByteBufBufferingSubscription(
    Publisher<? extends ByteBuf> upstream,
    Subscriber<? super ByteBuf> subscriber,
    ByteBufAllocator alloc,
    int maxNum,
    long watermark
  ) {
    super(subscriber, ByteBuf::release);
    this.upstream = upstream;
    this.alloc = alloc;
    this.maxNum = maxNum;
    this.watermark = watermark;
  }

  @Override
  protected void onRequest(long n) {
    if (subscription == null) {
      upstream.subscribe(new Subscriber<ByteBuf>() {
        @Override
        public void onSubscribe(Subscription s) {
          subscription = s;
          onConnected();
          subscription.request(n);
        }

        @Override
        public void onNext(ByteBuf t) {
          if (isDone()) {
            t.release();
            return;
          }

          if (t.readableBytes() == 0) {
            t.release();
            subscription.request(1);
            return;
          }

          addToBuffer(t);
          if (!maybeFlush()) {
            subscription.request(1);
          }
        }

        @Override
        public void onError(Throwable t) {
          if (buffer != null) {
            buffer.release();
            buffer = null;
          }
          emitError(t);
        }

        @Override
        public void onComplete() {
          if (buffer != null) {
            flush();
          }
          emitComplete();
        }
      });
    } else {
      subscription.request(n);
    }
  }

  protected boolean maybeFlush() {
    if (shouldFlush()) {
      flush();
      return true;
    } else {
      return false;
    }
  }

  protected boolean shouldFlush() {
    return buffer != null && (buffer.numComponents() == maxNum || buffer.readableBytes() >= watermark);
  }

  protected void flush() {
    if (buffer == null) {
      throw new IllegalStateException();
    }
    CompositeByteBuf emittedBuffer = this.buffer;
    this.buffer = null; // unset before emitting in case emit causes cancel, which would cause double release
    emitNext(emittedBuffer);
  }

  private void addToBuffer(ByteBuf t) {
    if (buffer == null) {
      buffer = alloc.compositeBuffer(maxNum);
    }
    buffer.addComponent(true, t);
  }

  protected void onConnected() {

  }

  protected boolean isEmpty() {
    return buffer == null;
  }

  @Override
  protected void onCancel() {
    if (buffer != null) {
      buffer.release();
    }
    if (subscription != null) {
      subscription.cancel();
    }
  }
}
