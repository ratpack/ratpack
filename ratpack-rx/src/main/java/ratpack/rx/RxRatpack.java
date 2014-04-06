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

package ratpack.rx;

import ratpack.func.Action;
import ratpack.promise.SuccessOrErrorPromise;
import ratpack.util.ExceptionUtils;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action2;

import java.util.concurrent.Callable;

/**
 * Provides static access Rx helper functions.
 */
public abstract class RxRatpack {

  @SuppressWarnings("UnusedDeclaration")
  private static RxBackground rxBackground; // set reflectively in RxModule.

  /**
   * Syntactic shorthand for calling {@link RxBackground#observe(Callable)}.
   * <p>
   * Usage of this method only works if there is one Ratpack application running in the JVM.
   * Results are undefined otherwise.
   * In such a case, use {@link RxBackground} instances directly.
   *
   * @param callable A blocking operation
   * @param <T> The type of value the blocking operation returns
   * @return An {@link rx.Observable} of the blocking operation
   */
  public static <T> Observable<T> background(Callable<T> callable) {
    if (rxBackground == null) {
      throw new IllegalStateException("Not initialized. Did you register the RxModule?");
    } else {
      return rxBackground.observe(callable);
    }
  }

  /**
   * Converts a Ratpack promise into an Rx observable.
   *
   * @param promise the promise
   * @param <T> the type of value promised
   * @return an observable for the promised value
   */
  public static <T> Observable<T> observe(SuccessOrErrorPromise<T> promise) {
    return Observable.create(new PromiseSubscribe<>(promise, new Action2<T, Subscriber<? super T>>() {
      @Override
      public void call(T thing, Subscriber<? super T> subscriber) {
        subscriber.onNext(thing);
      }
    }));
  }

  /**
   * Converts a Ratpack promise of an iterable value into an Rx observable for each element of the promised iterable.
   *
   * @param promise the promise
   * @param <T> the element type of the promised iterable
   * @param <I> the type of iterable
   * @return an observable for each element of the promised iterable
   */
  public static <T, I extends Iterable<T>> Observable<T> observeEach(SuccessOrErrorPromise<I> promise) {
    return Observable.create(new PromiseSubscribe<>(promise, new Action2<I, Subscriber<? super T>>() {
      @Override
      public void call(I things, Subscriber<? super T> subscriber) {
        for (T thing : things) {
          subscriber.onNext(thing);
        }
      }
    }));
  }

  private static class PromiseSubscribe<T, S> implements Observable.OnSubscribe<S> {
    private final SuccessOrErrorPromise<T> promise;
    private final Action2<T, Subscriber<? super S>> emitter;

    public PromiseSubscribe(SuccessOrErrorPromise<T> promise, Action2<T, Subscriber<? super S>> emitter) {
      this.promise = promise;
      this.emitter = emitter;
    }

    @Override
    public void call(final Subscriber<? super S> subscriber) {
      try {
        promise
          .onError(new Action<Throwable>() {
            @Override
            public void execute(Throwable throwable) throws Exception {
              subscriber.onError(throwable);
            }
          })
          .then(new Action<T>() {
            @Override
            public void execute(T thing) throws Exception {
              emitter.call(thing, subscriber);
              subscriber.onCompleted();
            }
          });
      } catch (Exception e) {
        throw ExceptionUtils.uncheck(e);
      }
    }
  }

}

