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
import ratpack.stream.TransformablePublisher;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

public class FanOutPublisher<T> implements TransformablePublisher<T> {

  private final Publisher<? extends Iterable<? extends T>> publisher;
  private final Action<? super T> disposer;

  public FanOutPublisher(Publisher<? extends Iterable<? extends T>> publisher, Action<? super T> disposer) {
    this.publisher = publisher;
    this.disposer = disposer;
  }

  enum State {
    UNSUBSCRIBED, PENDING_SUBSCRIBE, REQUESTED, IDLE, EMITTING
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    s.onSubscribe(new ManagedSubscription<T>(s, disposer) {

      Iterator<? extends T> iterator;
      Subscription subscription;

      AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

      @Override
      protected void onRequest(long n) {
        if (state.compareAndSet(State.UNSUBSCRIBED, State.PENDING_SUBSCRIBE)) {
          publisher.subscribe(new Subscriber<Iterable<? extends T>>() {
            @Override
            public void onSubscribe(Subscription s) {
              subscription = s;
              state.set(State.REQUESTED);
              s.request(1);
            }

            @Override
            public void onNext(Iterable<? extends T> ts) {
              iterator = ts.iterator();
              state.set(State.IDLE);
              drain();
            }

            @Override
            public void onError(Throwable t) {
              emitError(t);
              drain();
            }

            @Override
            public void onComplete() {
              subscription = null;
              state.compareAndSet(State.REQUESTED, State.IDLE);
              drain();
            }
          });
        } else if (iterator != null) {
          drain();
        }
      }

      private void drain() {
        if (state.compareAndSet(State.IDLE, State.EMITTING)) {
          if (isDone()) {
            if (iterator != null) {
              while (iterator.hasNext()) {
                dispose(iterator.next());
              }
            }
            return;
          }

          boolean hasNext = false;
          if (iterator != null) {
            while ((hasNext = iterator.hasNext()) && shouldEmit()) {
              emitNext(iterator.next());
            }
          }
          if (!hasNext) { // iterator is empty
            if (subscription == null) {
              emitComplete();
            } else if (hasDemand()) {
              state.set(State.REQUESTED);
              subscription.request(1);
              return;
            }
          }
          state.set(State.IDLE);
          if (hasDemand() || isDone()) {
            drain();
          }
        }
      }

      @Override
      protected void onCancel() {
        if (subscription != null) {
          subscription.cancel();
        }
      }
    });
  }
}
