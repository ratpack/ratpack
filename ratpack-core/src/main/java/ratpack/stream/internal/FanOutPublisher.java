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

package ratpack.stream.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.func.Action;

import java.util.concurrent.atomic.AtomicBoolean;

public class FanOutPublisher<T> extends BufferingPublisher<T> {

  public FanOutPublisher(Publisher<? extends Iterable<? extends T>> publisher) {
    super(Action.noop(), write -> {
      return new Subscription() {

        Subscription upstream;
        final AtomicBoolean emitting = new AtomicBoolean();

        @Override
        public void request(long n) {
          if (emitting.get()) {
            return;
          }

          if (upstream == null) {
            publisher.subscribe(new Subscriber<Iterable<? extends T>>() {
              @Override
              public void onSubscribe(Subscription s) {
                if (write.isCancelled()) {
                  s.cancel();
                  return;
                }

                upstream = s;
                if (write.getRequested() > 0) {
                  s.request(write.getRequested());
                }
              }

              @Override
              public void onNext(Iterable<? extends T> items) {
                emitting.set(true);
                for (T item : items) {
                  write.item(item);
                }
                emitting.set(false);
                long requested = write.getRequested();
                if (requested > 0) {
                  upstream.request(1);
                }
              }

              @Override
              public void onError(Throwable t) {
                write.error(t);
              }

              @Override
              public void onComplete() {
                write.complete();
              }
            });
          } else {
            upstream.request(n);
          }
        }

        @Override
        public void cancel() {
          if (upstream != null) {
            upstream.cancel();
          }
        }
      };
    });
  }

}
