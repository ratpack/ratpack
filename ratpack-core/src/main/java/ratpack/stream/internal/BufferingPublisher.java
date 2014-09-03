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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BufferingPublisher<T> implements Publisher<T> {

  private final AtomicLong wanted = new AtomicLong();
  private final ConcurrentLinkedQueue<T> buffer = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean finished = new AtomicBoolean();
  private final AtomicBoolean draining = new AtomicBoolean();

  private final Publisher<T> publisher;

  public BufferingPublisher(Publisher<T> publisher) {
    this.publisher = publisher;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    publisher.subscribe(new Subscriber<T>() {
      public Subscription subscription;

      private void tryDrain() {
        if (draining.compareAndSet(false, true)) {
          try {
            long i = wanted.get();
            while (i > 0) {
              T item = buffer.poll();
              if (item == null) {
                if (finished.get()) {
                  subscriber.onComplete();
                  return;
                } else {
                  break;
                }
              } else {
                subscriber.onNext(item);
                i = wanted.decrementAndGet();
              }
            }
          } finally {
            draining.set(false);
          }
          if (buffer.peek() != null && wanted.get() > 0) {
            tryDrain();
          }
        }
      }

      @Override
      public void onSubscribe(final Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);

        subscriber.onSubscribe(new Subscription() {
          @Override
          public void request(long n) {
            wanted.addAndGet(n);
            tryDrain();
          }

          @Override
          public void cancel() {
            finished.compareAndSet(false, true);
            subscription.cancel();
          }
        });
      }

      @Override
      public void onNext(T t) {
        buffer.add(t);
        tryDrain();
      }

      @Override
      public void onError(Throwable t) {
        buffer.clear();
        finished.compareAndSet(false, true);
        subscriber.onError(t);
      }

      @Override
      public void onComplete() {
        finished.compareAndSet(false, true);
        tryDrain();
      }
    });

  }

}
