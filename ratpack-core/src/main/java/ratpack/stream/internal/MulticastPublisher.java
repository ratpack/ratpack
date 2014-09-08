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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static ratpack.stream.Streams.throttle;

public class MulticastPublisher<T> implements Publisher<T> {

  private final List<Subscriber<? super T>> throttledSubscribers = new CopyOnWriteArrayList<>();
  private final Publisher<T> upstreamPublisher;
  private final AtomicBoolean requestedUpstream;

  public MulticastPublisher(Publisher<T> publisher) {
    this.upstreamPublisher = publisher;
    this.requestedUpstream = new AtomicBoolean();
  }

  @Override
  public void subscribe(final Subscriber<? super T> downStreamSubscriber) {
    throttle(new Publisher<T>() {
      @Override
      public void subscribe(final Subscriber<? super T> s) {
        s.onSubscribe(new Subscription() {
          @Override
          public void request(long n) {
            throttledSubscribers.add(s);
            tryUpstreamSubscribe();
          }

          @Override
          public void cancel() {
            throttledSubscribers.remove(s);
          }
        });
      }
    }).subscribe(downStreamSubscriber);
  }

  private void tryUpstreamSubscribe() {
    if (requestedUpstream.compareAndSet(false, true)) {
      this.upstreamPublisher.subscribe(new Subscriber<T>() {
        Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
          this.subscription = s;
          this.subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
          for (Subscriber<? super T> subscriber : throttledSubscribers) {
            subscriber.onNext(t);
          }
        }

        @Override
        public void onError(Throwable t) {
          for (Subscriber<? super T> subscriber : throttledSubscribers) {
            subscriber.onError(t);
          }
        }

        @Override
        public void onComplete() {
          for (Subscriber<? super T> subscriber : throttledSubscribers) {
            subscriber.onComplete();
          }
        }
      });
    }
  }
}
