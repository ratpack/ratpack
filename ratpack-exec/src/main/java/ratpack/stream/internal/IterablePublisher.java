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
import ratpack.func.Action;
import ratpack.stream.TransformablePublisher;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class IterablePublisher<T> implements TransformablePublisher<T> {

  private final Iterable<? extends T> iterable;

  public IterablePublisher(Iterable<? extends T> iterable) {
    this.iterable = iterable;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    Iterator<? extends T> iterator;
    try {
      iterator = iterable.iterator();
    } catch (Throwable e) {
      subscriber.onError(e);
      return;
    }

    AtomicBoolean draining = new AtomicBoolean();
    subscriber.onSubscribe(new ManagedSubscription<T>(subscriber, Action.noop()) {

      @Override
      protected void onRequest(long n) {
        if (draining.compareAndSet(false, true)) {
          if (isDone()) {
            try {
              while (iterator.hasNext()) {
                dispose(iterator.next());
              }
            } catch (Exception ignore) {
              // ignore
            }
            return;
          }

          try {
            while (--n >= 0 && iterator.hasNext() && !isDone()) {
              emitNext(iterator.next());
            }
          } catch (Exception e) {
            emitError(e);
            return;
          }

          if (!iterator.hasNext()) {
            emitComplete();
          }
          draining.set(false);
          n = getDemand();
          if (n > 0) {
            onRequest(n);
          }
        }
      }

      @Override
      protected void onCancel() {
        // nop
      }
    });
  }

}
