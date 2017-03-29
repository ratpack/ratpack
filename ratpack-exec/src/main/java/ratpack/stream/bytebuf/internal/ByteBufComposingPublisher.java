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

package ratpack.stream.bytebuf.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.internal.ManagedSubscription;

public class ByteBufComposingPublisher implements TransformablePublisher<CompositeByteBuf> {

  private enum State {Fetching, Writing, Closed}

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
  public void subscribe(Subscriber<? super CompositeByteBuf> subscriber) {
    subscriber.onSubscribe(new ManagedSubscription<CompositeByteBuf>(subscriber, ByteBuf::release) {

      private Subscription subscription;
      private CompositeByteBuf composite;

      private volatile State state;


      @Override
      protected void onRequest(long n) {
        if (subscription == null) {
          upstream.subscribe(new Subscriber<ByteBuf>() {

            @Override
            public void onSubscribe(Subscription s) {
              subscription = s;
              state = State.Fetching;
              s.request(1);
            }

            @Override
            public void onNext(ByteBuf t) {
              if (state == State.Closed) {
                t.release();
                return;
              }

              if (composite == null) {
                composite = alloc.compositeBuffer(maxNum);
              }
              composite.addComponent(true, t);
              if (composite.numComponents() == maxNum || composite.readableBytes() >= watermark) {
                state = State.Writing;
                emitNext(composite);
                composite = null;
                maybeFetch();
              } else {
                subscription.request(1);
              }
            }

            @Override
            public void onError(Throwable t) {
              state = State.Closed;
              ReferenceCountUtil.release(composite);
              emitError(t);
            }

            @Override
            public void onComplete() {
              state = State.Closed;

              if (composite != null) {
                emitNext(composite);
              }

              emitComplete();
            }
          });
        } else {
          maybeFetch();
        }
      }

      private void maybeFetch() {
        if (getDemand() > 0 && state != State.Fetching) {
          state = State.Fetching;
          subscription.request(1);
        }
      }

      @Override
      protected void onCancel() {
        state = State.Closed;
        ReferenceCountUtil.release(composite);
        if (subscription != null) {
          subscription.cancel();
        }
      }
    });
  }
}
