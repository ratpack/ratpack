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

package ratpack.rx.internal;

import ratpack.func.Action;
import ratpack.handling.Background;
import ratpack.rx.RxBackground;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action2;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class DefaultRxBackground implements RxBackground {

  private final Background background;

  @Inject
  public DefaultRxBackground(Background background) {
    this.background = background;
  }

  @Override
  public <T> Observable<T> observe(final Callable<T> callable) {
    return Observable.create(new OnSubscribe<>(callable, new Action2<T, Subscriber<? super T>>() {
      @Override
      public void call(T thing, Subscriber<? super T> subscriber) {
        subscriber.onNext(thing);
      }
    }));
  }

  @Override
  public <I extends Iterable<T>, T> Observable<T> observeEach(final Callable<I> callable) {
    return Observable.create(new OnSubscribe<>(callable, new Action2<I, Subscriber<? super T>>() {
      @Override
      public void call(I things, Subscriber<? super T> subscriber) {
        for (T thing : things) {
          subscriber.onNext(thing);
        }
      }
    }));
  }

  private class OnSubscribe<T, S> implements Observable.OnSubscribe<S> {
    private final Callable<T> callable;
    private final Action2<T, Subscriber<? super S>> emitter;

    public OnSubscribe(Callable<T> callable, Action2<T, Subscriber<? super S>> emitter) {
      this.callable = callable;
      this.emitter = emitter;
    }

    @Override
    public void call(final Subscriber<? super S> subscriber) {
      background.exec(callable)
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
    }
  }
}

