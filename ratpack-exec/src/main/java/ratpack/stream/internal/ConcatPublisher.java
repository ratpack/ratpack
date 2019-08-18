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

public class ConcatPublisher<T> implements TransformablePublisher<T> {

  private final Iterable<? extends Publisher<? extends T>> publishers;
  private final Action<? super T> disposer;

  public ConcatPublisher(Action<? super T> disposer, Iterable<? extends Publisher<? extends T>> publishers) {
    this.publishers = publishers;
    this.disposer = disposer;
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    s.onSubscribe(new ManagedSubscription<T>(s, disposer) {

      Iterator<? extends Publisher<? extends T>> iterator = publishers.iterator();
      Subscription current;

      @Override
      protected void onRequest(long n) {
        if (current == null) {
          if (iterator.hasNext()) {
            Publisher<? extends T> publisher = iterator.next();
            publisher.subscribe(new Subscriber<T>() {
              @Override
              public void onSubscribe(Subscription s) {
                current = s;
                s.request(n);
              }

              @Override
              public void onNext(T t) {
                emitNext(t);
              }

              @Override
              public void onError(Throwable t) {
                emitError(t);
              }

              @Override
              public void onComplete() {
                current = null;
                long demand = getDemand();
                if (demand > 0) {
                  onRequest(demand);
                }
              }
            });
          } else {
            emitComplete();
          }
        } else {
          current.request(n);
        }
      }

      @Override
      protected void onCancel() {
        if (current != null) {
          current.cancel();
        }
      }
    });
  }

}
