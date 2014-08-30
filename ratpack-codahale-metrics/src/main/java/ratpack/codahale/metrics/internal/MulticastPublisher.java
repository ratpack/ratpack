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

package ratpack.codahale.metrics.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A Reactive Streams compliant {@link org.reactivestreams.Publisher} for publishing to multiple {@link org.reactivestreams.Subscriber}.
 * @param <T> the Type of element being published
 */
public class MulticastPublisher<T> implements Publisher<T> {

  private final List<Subscriber<T>> subscribers = new CopyOnWriteArrayList<>();

  public void broadcast(T element) {
    for (Subscriber<T> subscriber : subscribers) {
      subscriber.onNext(element);
    }
  }

  @Override
  public void subscribe(final Subscriber<T> s) {
    s.onSubscribe(new Subscription() {
      @Override
      public void request(int n) {
        if (n != Integer.MAX_VALUE) {
          throw new IllegalArgumentException("Back pressure is not supported by this Publisher.  Request with Integer.MAX_VALUE only");
        }
        subscribers.add(s);
      }

      @Override
      public void cancel() {
        subscribers.remove(s);
      }
    });
  }
}
