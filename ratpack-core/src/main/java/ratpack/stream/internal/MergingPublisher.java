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

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MergingPublisher<T> implements TransformablePublisher<T> {

  private final ConcurrentLinkedDeque<Publisher<? extends T>> upstreamPublishers = new ConcurrentLinkedDeque<>();
  private final AtomicInteger publisherCount = new AtomicInteger();
  private final ConcurrentLinkedDeque<Subscription> upstreamPublisherSubscriptions = new ConcurrentLinkedDeque<>();
  private Subscriber<? super T> downstreamSubscriber;

  @SafeVarargs
  public MergingPublisher(Publisher<? extends T>... publishers) {
    if (publishers.length < 2) {
      throw new IllegalArgumentException("At least 2 publishers must be supplied to merge");
    }

    publisherCount.set(publishers.length);
    //noinspection ManualArrayToCollectionCopy
    for (Publisher<? extends T> publisher : publishers) {
      upstreamPublishers.add(publisher);
    }
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    this.downstreamSubscriber = subscriber;

    for (final Publisher<? extends T> upstreamPublisher : upstreamPublishers) {
      upstreamPublisher.subscribe(new Subscriber<T>() {
        final AtomicBoolean finished = new AtomicBoolean();
        Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
          subscription = s;
          upstreamPublisherSubscriptions.add(s);
        }

        @Override
        public void onNext(T t) {
          downstreamSubscriber.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
          if (finished.compareAndSet(false, true)) {
            upstreamPublisherSubscriptions.stream()
              .filter(upstreamPublisherSubscription -> upstreamPublisherSubscription != subscription)
              .forEach(Subscription::cancel);
            upstreamPublisherSubscriptions.clear();
            upstreamPublishers.clear();
            downstreamSubscriber.onError(t);
          }
        }

        @Override
        public void onComplete() {
          if (finished.compareAndSet(false, true)) {
            upstreamPublishers.remove(upstreamPublisher);
            upstreamPublisherSubscriptions.remove(subscription);
            tryComplete();
          }
        }
      });
    }

    subscriber.onSubscribe(new Subscription() {
      @Override
      public void request(long n) {
        for (Subscription upstreamPublisherSubscription : upstreamPublisherSubscriptions) {
          upstreamPublisherSubscription.request(n);
        }
      }

      @Override
      public void cancel() {
        for (Subscription upstreamPublisherSubscription : upstreamPublisherSubscriptions) {
          upstreamPublisherSubscription.cancel();
          upstreamPublisherSubscriptions.remove(this);
        }
        upstreamPublishers.clear();
        downstreamSubscriber = null;
      }
    });
  }

  private void tryComplete() {
    if (publisherCount.decrementAndGet() == 0) {
      downstreamSubscriber.onComplete();
    }
  }

}
