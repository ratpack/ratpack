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

package ratpack.stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.LinkedList;
import java.util.List;

public class CollectingSubscriber<T> implements Subscriber<T> {
  public final List<T> received = new LinkedList<T>();
  public Subscription subscription;
  public Throwable error;
  public boolean complete;

  public static <T> CollectingSubscriber<T> subscribe(Publisher<T> publisher) {
    CollectingSubscriber<T> subscriber = new CollectingSubscriber<T>();
    publisher.subscribe(subscriber);
    return subscriber;
  }

  @Override
  public void onSubscribe(Subscription s) {
    subscription = s;
  }

  @Override
  public void onNext(T t) {
    received.add(t);
  }

  @Override
  public void onError(Throwable t) {
    error = t;
  }

  @Override
  public void onComplete() {
    complete = true;
  }
}
