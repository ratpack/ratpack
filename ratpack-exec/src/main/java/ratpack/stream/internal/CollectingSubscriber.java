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

import com.google.common.collect.Lists;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.Result;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class CollectingSubscriber<T> implements Subscriber<T> {

  public final List<T> received = Lists.newLinkedList();
  public Subscription subscription;
  public Throwable error;
  public boolean complete;
  public final Consumer<? super Result<List<T>>> consumer;
  private final Consumer<? super Subscription> subscriptionConsumer;

  public CollectingSubscriber() {
    this(r -> {
    }, s -> {
    });
  }

  public CollectingSubscriber(Consumer<? super Result<List<T>>> resultConsumer, Consumer<? super Subscription> subscriptionConsumer) {
    this.consumer = resultConsumer;
    this.subscriptionConsumer = subscriptionConsumer;
  }

  public static <T> CollectingSubscriber<T> subscribe(Publisher<T> publisher) {
    CollectingSubscriber<T> subscriber = new CollectingSubscriber<>();
    publisher.subscribe(subscriber);
    return subscriber;
  }

  @Override
  public void onSubscribe(Subscription s) {
    subscription = s;
    subscriptionConsumer.accept(s);
  }

  @Override
  public void onNext(T t) {
    received.add(t);
  }

  @Override
  public void onError(Throwable t) {
    error = t;
    consumer.accept(Result.<List<T>>error(t));
  }

  @Override
  public void onComplete() {
    complete = true;
    consumer.accept(Result.success(Collections.unmodifiableList(received)));
  }

}
