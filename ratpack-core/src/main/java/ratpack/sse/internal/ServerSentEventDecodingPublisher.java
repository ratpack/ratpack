/*
 * Copyright 2016 the original author or authors.
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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.func.Action;
import ratpack.sse.Event;
import ratpack.stream.internal.BufferingPublisher;

public class ServerSentEventDecodingPublisher extends BufferingPublisher<Event<?>> {

  public ServerSentEventDecodingPublisher(Publisher<? extends ByteBuf> publisher, ByteBufAllocator allocator) {
    super(Action.noop(), write -> {
      return new Subscription() {

        Subscription upstream;
        ServerSentEventDecoder decoder = new ServerSentEventDecoder(allocator, write::item);

        volatile boolean emitting;

        @Override
        public void request(long n) {
          if (emitting) {
            return;
          }

          if (upstream == null) {
            publisher.subscribe(new Subscriber<ByteBuf>() {
              @Override
              public void onSubscribe(Subscription s) {
                upstream = s;
                upstream.request(n);
              }

              @Override
              public void onNext(ByteBuf event) {
                emitting = true;
                try {
                  decoder.decode(event);
                } catch (Throwable e) {
                  upstream.cancel();
                  onError(e);
                  return;
                } finally {
                  emitting = false;
                }
                if (write.getRequested() > 0) {
                  upstream.request(1);
                }
              }

              @Override
              public void onError(Throwable t) {
                decoder.close();
                write.error(t);
              }

              @Override
              public void onComplete() {
                decoder.close();
                write.complete();
              }
            });
          } else {
            upstream.request(n);
          }

        }

        @Override
        public void cancel() {
          decoder.close();
          if (upstream != null) {
            upstream.cancel();
          }
        }
      };
    });
  }

}
