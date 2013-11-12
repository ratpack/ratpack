/*
 * Copyright 2013 the original author or authors.
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

package ratpack.groovy.block.internal;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.groovy.block.Blocking;
import ratpack.groovy.handling.GroovyContext;
import ratpack.groovy.internal.Util;
import ratpack.util.Action;

import java.util.concurrent.Callable;

public class GroovyBlocking implements Blocking {

  private final GroovyContext context;
  private final ratpack.block.Blocking delegate;

  public GroovyBlocking(GroovyContext context, ratpack.block.Blocking delegate) {
    this.context = context;
    this.delegate = delegate;
  }

  @Override
  public <T> ratpack.block.Blocking.SuccessOrError<T> exec(Callable<T> operation) {
    return delegate.exec(operation);
  }

  @Override
  public <T> SuccessOrError<T> block(Closure<T> closure) {
    final ratpack.block.Blocking.SuccessOrError<T> successOrErrorDelegate = delegate.exec(Util.delegatingCallable(closure));
    return new SuccessOrErrorImpl<>(successOrErrorDelegate);
  }

  private class SuccessImpl<T> implements Success<T> {

    private final ratpack.block.Blocking.Success<T> successDelegate;

    private SuccessImpl(ratpack.block.Blocking.Success<T> successDelegate) {
      this.successDelegate = successDelegate;
    }

    @Override
    public void then(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
      then(Util.fixedDelegatingAction(context, closure));
    }

    @Override
    public void then(Action<? super T> then) {
      successDelegate.then(then);
    }
  }

  private class SuccessOrErrorImpl<T> extends SuccessImpl<T> implements SuccessOrError<T> {
    private final ratpack.block.Blocking.SuccessOrError<T> successOrErrorDelegate;

    public SuccessOrErrorImpl(ratpack.block.Blocking.SuccessOrError<T> successOrErrorDelegate) {
      super(successOrErrorDelegate);
      this.successOrErrorDelegate = successOrErrorDelegate;
    }

    @Override
    public Success<T> onError(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
      return new SuccessImpl<>(successOrErrorDelegate.onError(Util.action(closure)));
    }

    @Override
    public ratpack.block.Blocking.Success<T> onError(Action<? super Throwable> errorHandler) {
      return successOrErrorDelegate.onError(errorHandler);
    }
  }
}
