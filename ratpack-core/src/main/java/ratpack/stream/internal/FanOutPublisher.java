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
import ratpack.stream.TransformablePublisher;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class FanOutPublisher<T> implements TransformablePublisher<T> {

  private final Publisher<? extends Iterable<? extends T>> upstream;

  public FanOutPublisher(Publisher<? extends Iterable<? extends T>> upstream) {
    this.upstream = upstream;
  }

  // Note, we know that a buffer publisher is downstream

  @Override
  public void subscribe(final Subscriber<? super T> downstream) {
    upstream.subscribe(new Subscriber<Iterable<? extends T>>() {

      private final AtomicBoolean done = new AtomicBoolean();
      private Subscription upstreamSubscription;

      @Override
      public void onSubscribe(Subscription subscription) {
        upstreamSubscription = subscription;

        downstream.onSubscribe(new Subscription() {
          @Override
          public void request(long n) {
            upstreamSubscription.request(n);
          }

          @Override
          public void cancel() {
            done.set(true);
            upstreamSubscription.cancel();
          }
        });
      }

      @Override
      public void onNext(Iterable<? extends T> iterable) {
        Iterator<? extends T> iterator = iterable.iterator();

        if (!iterator.hasNext() && !done.get()) {
          upstreamSubscription.request(1); // nothing in this iterable, request another to meet demand
        } else {
          while (iterator.hasNext() && !done.get()) {
            downstream.onNext(iterator.next());
          }
        }
      }

      @Override
      public void onError(Throwable t) {
        downstream.onError(t);
      }

      @Override
      public void onComplete() {
        downstream.onComplete();
      }
    });
  }

}
