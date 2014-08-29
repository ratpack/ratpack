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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.stream.internal.BufferingPublisher;
import ratpack.stream.internal.PeriodicPublisher;

import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Streams {

  private final static Logger LOGGER = LoggerFactory.getLogger(Streams.class);

  public static <I, O> Publisher<O> transform(final Publisher<I> input, final Function<? super I, ? extends O> function) {
    return new Publisher<O>() {
      @Override
      public void subscribe(final Subscriber<O> outSubscriber) {
        input.subscribe(new Subscriber<I>() {

          private Subscription subscription;
          private final AtomicBoolean done = new AtomicBoolean();

          @Override
          public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            outSubscriber.onSubscribe(this.subscription);
          }

          @Override
          public void onNext(I in) {
            O out;
            try {
              out = function.apply(in);
            } catch (Throwable throwable) {
              subscription.cancel();
              onError(throwable);
              return;
            }

            if (!done.get()) {
              outSubscriber.onNext(out);
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
    };
  }

  public static <T> Publisher<T> buffer(final Publisher<T> publisher) {
    return new BufferingPublisher<>(publisher);
  }

  public static <T> Publisher<T> periodically(ScheduledExecutorService executorService, final long delay, final TimeUnit timeUnit, final Function<Integer, T> producer) {
    return buffer(new PeriodicPublisher<>(executorService, producer, delay, timeUnit));
  }

  public static <T> Publisher<T> wiretap(final Publisher<T> publisher, final Action<? super T> listener) {
    return new Publisher<T>() {

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
    };
  }

  public static <T> Publisher<T> onCancel(final Publisher<T> publisher, final Action<Void> listener) {
    return new Publisher<T>() {
      @Override
      public void subscribe(final Subscriber<T> outSubscriber) {
        publisher.subscribe(new Subscriber<T>() {

          @Override
          public void onSubscribe(final Subscription subscription) {
            outSubscriber.onSubscribe(new Subscription() {
              @Override
              public void request(int n) {
                subscription.request(n);
              }

              @Override
              public void cancel() {
                try {
                  listener.execute(null);
                } catch (Throwable e) {
                  LOGGER.warn("ignoring exception thrown by cancel listener", e);
                } finally {
                  subscription.cancel();
                }
              }
            });
          }

          @Override
          public void onNext(T in) {
            outSubscriber.onNext(in);
          }

          @Override
          public void onError(Throwable t) {
            outSubscriber.onError(t);
          }

          @Override
          public void onComplete() {
            outSubscriber.onComplete();
          }
        });
      }
    };
  }

  public static <T> Publisher<T> publisher(Iterable<T> iterable) {
    return new IterablePublisher<>(iterable);
  }

  private static class IterablePublisher<T> implements Publisher<T> {

    private final Iterable<T> iterable;

    private IterablePublisher(Iterable<T> iterable) {
      this.iterable = iterable;
    }

    @Override
    public void subscribe(final Subscriber<T> subscriber) {
      subscriber.onSubscribe(new Subscription() {

        Iterator<T> iterator = iterable.iterator();

        @Override
        public void request(int n) {
          for (int i = 0; i < n; ++i) {
            if (iterator.hasNext()) {
              T next;
              try {
                next = iterator.next();
              } catch (Exception e) {
                subscriber.onError(e);
                return;
              }
              subscriber.onNext(next);
            } else {
              break;
            }
          }

          if (!iterator.hasNext()) {
            subscriber.onComplete();
          }
        }

        @Override
        public void cancel() {

        }
      });

    }
  }

}
