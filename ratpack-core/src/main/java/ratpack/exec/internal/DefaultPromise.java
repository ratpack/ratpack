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

package ratpack.exec.internal;

import ratpack.exec.Fulfiller;
import ratpack.exec.Promise;
import ratpack.exec.SuccessPromise;
import ratpack.func.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static ratpack.func.Actions.throwException;

public class DefaultPromise<T> implements Promise<T> {
  private final Action<? super Fulfiller<T>> fulfillment;
  private final Factory<ExecutionBacking> executionProvider;
  private final AtomicBoolean fired = new AtomicBoolean();

  public DefaultPromise(Factory<ExecutionBacking> executionProvider, Action<? super Fulfiller<T>> fulfillment) {
    this.executionProvider = executionProvider;
    this.fulfillment = fulfillment;
  }

  @Override
  public SuccessPromise<T> onError(final Action<? super Throwable> errorHandler) {
    if (fired.compareAndSet(false, true)) {
      return new DefaultSuccessPromise<>(executionProvider, fulfillment, errorHandler);
    } else {
      throw new MultiplePromiseSubscriptionException();
    }
  }

  @Override
  public void then(Action<? super T> then) {
    propagatingSuccessPromise().then(then);
  }

  private SuccessPromise<T> propagatingSuccessPromise() {
    return onError(throwException());
  }

  @Override
  public <O> Promise<O> map(Function<? super T, ? extends O> transformer) {
    return propagatingSuccessPromise().map(transformer);
  }

  @Override
  public <O> Promise<O> blockingMap(Function<? super T, ? extends O> transformer) {
    return propagatingSuccessPromise().blockingMap(transformer);
  }

  @Override
  public <O> Promise<O> flatMap(Function<? super T, ? extends Promise<O>> transformer) {
    return propagatingSuccessPromise().flatMap(transformer);
  }

  @Override
  public Promise<T> route(Predicate<? super T> predicate, Action<? super T> action) {
    return propagatingSuccessPromise().route(predicate, action);
  }

  @Override
  public Promise<T> onNull(NoArgAction action) {
    return propagatingSuccessPromise().onNull(action);
  }

}
