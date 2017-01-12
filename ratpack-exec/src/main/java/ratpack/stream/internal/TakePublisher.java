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

import java.util.concurrent.atomic.AtomicLong;

public final class TakePublisher<T> implements TransformablePublisher<T> {

  private final AtomicLong count;
  private final Publisher<T> upstreamPublisher;

  public TakePublisher(long count, Publisher<T> upstreamPublisher) {
    this.count = new AtomicLong(count);
    this.upstreamPublisher = upstreamPublisher;
  }

  @Override
  public void subscribe(Subscriber<? super T> downstreamSubscriber) {
    upstreamPublisher.subscribe(new Subscriber<T>() {

      private Subscription subscription;

      @Override
      public void onSubscribe(Subscription upstreamPublisherSubscription) {
        subscription = upstreamPublisherSubscription;
        downstreamSubscriber.onSubscribe(upstreamPublisherSubscription);
      }

      @Override
      public void onNext(T t) {
        long i = count.decrementAndGet();
        if (i >= 0) {
          downstreamSubscriber.onNext(t);
        }
        if (i == 0) {
          subscription.cancel();
          onComplete();
        }
      }

      @Override
      public void onError(Throwable t) {
        downstreamSubscriber.onError(t);
      }

      @Override
      public void onComplete() {
        downstreamSubscriber.onComplete();
      }
    });
  }
}
