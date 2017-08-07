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

import io.reactivex.FlowableSubscriber;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.exceptions.UndeliverableException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.Promise;
import ratpack.func.Action;

public class ExecutionBackedSubscriber<T> implements FlowableSubscriber<T> {
  private final Subscriber<? super T> subscriber;

  public ExecutionBackedSubscriber(Subscriber<T> subscriber) {
    this.subscriber = subscriber;
  }

  @Override
  public void onComplete() {
    try {
      subscriber.onComplete();
    } catch (final OnErrorNotImplementedException e) {
      Promise.error(e.getCause()).then(Action.noop());
    } catch (Exception e) {
      Promise.error(new UndeliverableException(e)).then(Action.noop());
    }
  }

  @Override
  public void onError(Throwable e) {
    try {
      subscriber.onError(e);
    } catch (final OnErrorNotImplementedException e2) {
      Promise.error(e2.getCause()).then(Action.noop());
    } catch (Exception e2) {
      Promise.error(new CompositeException(e, e2)).then(Action.noop());
    }
  }

  @Override
  public void onSubscribe(Subscription s) {
    try {
      subscriber.onSubscribe(s);
    } catch (final OnErrorNotImplementedException e) {
      Promise.error(e.getCause()).then(Action.noop());
    } catch (Exception e) {
      Promise.error(new UndeliverableException(e)).then(Action.noop());
    }
  }

  @Override
  public void onNext(T t) {
    try {
      subscriber.onNext(t);
    } catch (final OnErrorNotImplementedException e) {
      Promise.error(e.getCause()).then(Action.noop());
    } catch (Exception e) {
      Promise.error(new UndeliverableException(e)).then(Action.noop());
    }
  }
}
