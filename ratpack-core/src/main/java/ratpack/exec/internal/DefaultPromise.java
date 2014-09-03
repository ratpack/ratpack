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
import ratpack.func.Action;
import ratpack.func.Actions;
import ratpack.func.Factory;
import ratpack.func.Function;

public class DefaultPromise<T> implements Promise<T> {
  private final Action<? super Fulfiller<T>> fulfillment;
  private final Factory<ExecutionBacking> executionProvider;

  public DefaultPromise(Factory<ExecutionBacking> executionProvider, Action<? super Fulfiller<T>> fulfillment) {
    this.executionProvider = executionProvider;
    this.fulfillment = fulfillment;
  }

  @Override
  public SuccessPromise<T> onError(final Action<? super Throwable> errorHandler) {
    return new DefaultSuccessPromise<>(executionProvider, fulfillment, errorHandler);
  }

  @Override
  public void then(Action<? super T> then) {
    propagatingSuccessPromise().then(then);
  }

  private SuccessPromise<T> propagatingSuccessPromise() {
    return onError(Actions.throwException());
  }

  @Override
  public <O> Promise<O> map(Function<? super T, ? extends O> function) {
    return propagatingSuccessPromise().map(function);
  }

  @Override
  public <O> Promise<O> flatMap(Function<? super T, ? extends Promise<O>> function) {
    return propagatingSuccessPromise().flatMap(function);
  }

}
