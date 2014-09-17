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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public class FanOutPublisher<T> implements Publisher<T> {

  private final Publisher<Collection<T>> upstreamIterablePublisher;

  public FanOutPublisher(Publisher<Collection<T>> publisher) {
    this.upstreamIterablePublisher = publisher;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    upstreamIterablePublisher.subscribe(new Subscriber<Iterable<T>>() {

      private Subscription subscription;
      private final AtomicBoolean done = new AtomicBoolean();

      @Override
      public void onSubscribe(Subscription s) {
        this.subscription = s;
        subscriber.onSubscribe(this.subscription);
      }

      @Override
      public void onNext(Iterable<T> iterable) {
        for (T element: iterable) {
          subscriber.onNext(element);
        }
      }

      @Override
      public void onError(Throwable t) {
        if (done.compareAndSet(false, true)) {
          subscriber.onError(t);
        }
      }

      @Override
      public void onComplete() {
        if (done.compareAndSet(false, true)) {
          subscriber.onComplete();
        }
      }
    });
  }

}
