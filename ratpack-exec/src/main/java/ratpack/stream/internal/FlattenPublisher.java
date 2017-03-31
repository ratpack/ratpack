/*
 * Copyright 2017 the original author or authors.
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
import ratpack.stream.TransformablePublisher;

import java.util.concurrent.atomic.AtomicReference;

public class FlattenPublisher<T> implements TransformablePublisher<T> {

  private final Publisher<? extends Publisher<T>> publisher;
  private final Action<? super T> disposer;

  public FlattenPublisher(Publisher<? extends Publisher<T>> publisher, Action<? super T> disposer) {
    this.publisher = publisher;
    this.disposer = disposer;
  }

  enum State {
    INIT, SUBSCRIBE, IDLE, PENDING, EMITTING
  }

  @Override
  public void subscribe(Subscriber<? super T> subscriber) {
    subscriber.onSubscribe(new ManagedSubscription<T>(subscriber, disposer) {

      private Subscription outerSubscription;
      private Subscription innerSubscription;

      private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

      volatile boolean pendingComplete;

      @Override
      protected void onRequest(long n) {
        if (state.compareAndSet(State.INIT, State.SUBSCRIBE)) {
          if (outerSubscription == null) {
            subscribeUpstream();
          }
        } else if (innerSubscription != null) {
          innerSubscription.request(n);
        } else {
          nextPublisher();
        }
      }

      private void subscribeUpstream() {
        publisher.subscribe(new Subscriber<Publisher<T>>() {
          @Override
          public void onSubscribe(Subscription subscription) {
            outerSubscription = subscription;
            outerSubscription.request(1);
          }

          @Override
          public void onNext(Publisher<T> next) {
            next.subscribe(new Subscriber<T>() {
              @Override
              public void onSubscribe(Subscription s) {
                innerSubscription = s;
                state.set(State.EMITTING);
                innerSubscription.request(getDemand());
              }

              @Override
              public void onNext(T t) {
                emitNext(t);
              }

              @Override
              public void onError(Throwable t) {
                outerSubscription.cancel();
                emitError(t);
              }

              @Override
              public void onComplete() {
                innerSubscription = null;
                state.set(State.IDLE);
                nextPublisher();
              }
            });
          }

          @Override
          public void onError(Throwable t) {
            if (innerSubscription != null) {
              innerSubscription.cancel();
              innerSubscription = null;
            }
            emitError(t);
          }

          @Override
          public void onComplete() {
            pendingComplete = true;
            nextPublisher();
          }
        });
      }

      @Override
      protected void onCancel() {
        if (innerSubscription != null) {
          innerSubscription.cancel();
          innerSubscription = null;
        }
        if (outerSubscription != null) {
          outerSubscription.cancel();
          outerSubscription = null;
        }

      }

      private void nextPublisher() {
        if (state.compareAndSet(State.IDLE, State.PENDING)) {
          if (pendingComplete) {
            emitComplete();
          } else if (hasDemand()) {
            outerSubscription.request(1);
          } else {
            state.set(State.IDLE);
            if (hasDemand()) {
              nextPublisher();
            }
          }
        } else if (state.get() == State.PENDING && pendingComplete) {
          emitComplete();
        }

      }
    });

  }

}
