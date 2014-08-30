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
import ratpack.func.Action;

import java.util.concurrent.atomic.AtomicBoolean;

public class WiretapPublisher<T> implements Publisher<T> {

  private final Publisher<T> publisher;
  private final Action<? super T> listener;

  public WiretapPublisher(Publisher<T> publisher, Action<? super T> listener) {
    this.publisher = publisher;
    this.listener = listener;
  }

  @Override
  public void subscribe(final Subscriber<T> outSubscriber) {
    publisher.subscribe(new Subscriber<T>() {

      private Subscription subscription;
      private final AtomicBoolean done = new AtomicBoolean();

      @Override
      public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        outSubscriber.onSubscribe(this.subscription);
      }

      @Override
      public void onNext(T in) {
        try {
          listener.execute(in);
        } catch (Throwable throwable) {
          subscription.cancel();
          onError(throwable);
          return;
        }

        if (!done.get()) {
          outSubscriber.onNext(in);
        }
      }

      @Override
      public void onError(Throwable t) {
        if (done.compareAndSet(false, true)) {
          outSubscriber.onError(t);
        }
      }

      @Override
      public void onComplete() {
        if (done.compareAndSet(false, true)) {
          outSubscriber.onComplete();
        }
      }
    });
  }
}
