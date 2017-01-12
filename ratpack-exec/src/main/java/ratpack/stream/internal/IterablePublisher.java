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

import org.reactivestreams.Subscriber;
import ratpack.stream.TransformablePublisher;

import java.util.Iterator;

public class IterablePublisher<T> implements TransformablePublisher<T> {

  private final Iterable<? extends T> iterable;

  public IterablePublisher(Iterable<? extends T> iterable) {
    this.iterable = iterable;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    final Iterator<? extends T> iterator = iterable.iterator();
    new Subscription(subscriber, iterator);
  }

  private class Subscription extends SubscriptionSupport<T> {
    private final Iterator<? extends T> iterator;

    public Subscription(Subscriber<? super T> subscriber, Iterator<? extends T> iterator) {
      super(subscriber);
      this.iterator = iterator;
      start();
    }

    @Override
    protected void doRequest(long n) {
      for (int i = 0; i < n && !isStopped(); ++i) {
        if (iterator.hasNext()) {
          T next;
          try {
            next = iterator.next();
          } catch (Exception e) {
            onError(e);
            return;
          }
          onNext(next);
        } else {
          onComplete();
        }
      }
    }

  }
}
