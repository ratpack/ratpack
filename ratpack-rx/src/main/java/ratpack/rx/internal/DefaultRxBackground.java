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

import ratpack.handling.Background;
import ratpack.rx.RxBackground;
import ratpack.util.Action;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

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
    return Observable.create(new Observable.OnSubscribeFunc<T>() {
      @Override
      public Subscription onSubscribe(final Observer<? super T> observer) {
        background.exec(callable).onError(new Action<Throwable>() {
          @Override
          public void execute(Throwable thing) throws Exception {
            observer.onError(thing);
          }
        }).then(new Action<T>() {
          @Override
          public void execute(T thing) throws Exception {
            observer.onNext(thing);
            observer.onCompleted();
          }
        });
        return Subscriptions.empty();
      }
    });
  }

  @Override
  public <I extends Iterable<T>, T> Observable<T> observeEach(final Callable<I> callable) {
    return Observable.create(new Observable.OnSubscribeFunc<T>() {
      @Override
      public Subscription onSubscribe(final Observer<? super T> observer) {
        background.exec(callable).onError(new Action<Throwable>() {
          @Override
          public void execute(Throwable thing) throws Exception {
            observer.onError(thing);
          }
        }).then(new Action<I>() {
          @Override
          public void execute(I things) throws Exception {
            for (T thing : things) {
              observer.onNext(thing);
            }

            observer.onCompleted();
          }
        });
        return Subscriptions.empty();
      }
    });
  }

}

