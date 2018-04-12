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

package ratpack.rx2.internal;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.exceptions.UndeliverableException;
import ratpack.exec.Promise;
import ratpack.func.Action;

public  class ExecutionBackedObserver<T> implements Observer<T> {
  private final Observer<? super T> observer;

  public ExecutionBackedObserver(Observer<? super T> observer) {
    this.observer = observer;
  }

  @Override
  public void onComplete() {
    try {
      observer.onComplete();
    } catch (final OnErrorNotImplementedException e) {
      Promise.error(e.getCause()).then(Action.noop());
    } catch (Exception e) {
      Promise.error(new UndeliverableException(e)).then(Action.noop());
    }
  }

  @Override
  public void onError(Throwable e) {
    try {
      observer.onError(e);
    } catch (final OnErrorNotImplementedException e2) {
      Promise.error(e2.getCause()).then(Action.noop());
    } catch (Exception e2) {
      Promise.error(new CompositeException(e, e2)).then(Action.noop());
    }
  }

  @Override
  public void onSubscribe(Disposable d) {
    try {
      observer.onSubscribe(d);
    } catch (final OnErrorNotImplementedException e) {
      Promise.error(e.getCause()).then(Action.noop());
    } catch (Exception e) {
      Promise.error(new UndeliverableException(e)).then(Action.noop());
    }
  }

  @Override
  public void onNext(T t) {
    try {
      observer.onNext(t);
    } catch (final OnErrorNotImplementedException e) {
      Promise.error(e.getCause()).then(Action.noop());
    } catch (Exception e) {
      Promise.error(new UndeliverableException(e)).then(Action.noop());
    }
  }

}
