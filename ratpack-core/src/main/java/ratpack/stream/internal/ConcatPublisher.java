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

import java.util.Iterator;

public final class ConcatPublisher<T> extends BufferingPublisher<T> {

  public ConcatPublisher(Iterable<Publisher<? extends T>> publishers) {
    super(Action.noop(), write -> {
      return new Subscription() {

        private final Iterator<Publisher<? extends T>> iterator = publishers.iterator();
        private Subscription subscription;
        private boolean open;

        @Override
        public void request(long n) {
          if (n == Long.MAX_VALUE) {
            open = true;
          }

          if (subscription == null) {
            next();
          } else {
            subscription.request(n);
          }

        }

        private void next() {
          if (iterator.hasNext()) {
            Publisher<? extends T> publisher = iterator.next();
            publisher.subscribe(new Subscriber<T>() {
              @Override
              public void onSubscribe(Subscription s) {
                subscription = s;
                if (open) {
                  s.request(Long.MAX_VALUE);
                } else {
                  long requested = write.getRequested();
                  if (requested > 0) {
                    s.request(requested);
                  }
                }
              }

              @Override
              public void onNext(T t) {
                write.item(t);
              }

              @Override
              public void onError(Throwable t) {
                write.error(t);
              }

              @Override
              public void onComplete() {
                next();
              }
            });
          } else {
            write.complete();
          }
        }

        @Override
        public void cancel() {
          if (subscription != null) {
            subscription.cancel();
          }
        }
      };
    });
  }

}
