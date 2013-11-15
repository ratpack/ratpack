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

package ratpack.groovy.background.internal;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.background.Background;
import ratpack.groovy.background.GroovyBackground;
import ratpack.groovy.handling.GroovyContext;
import ratpack.groovy.internal.Util;
import ratpack.util.Action;

import java.util.concurrent.Callable;

public class DefaultGroovyBackground implements GroovyBackground {

  private final GroovyContext context;
  private final Background delegate;

  public DefaultGroovyBackground(GroovyContext context, Background delegate) {
    this.context = context;
    this.delegate = delegate;
  }

  @Override
  public <T> SuccessOrError<T> exec(Callable<T> operation) {
    return delegate.exec(operation);
  }

  @Override
  public <T> GroovySuccessOrError<T> block(Closure<T> closure) {
    final SuccessOrError<T> successOrErrorDelegate = delegate.exec(Util.delegatingCallable(closure));
    return new GroovySuccessOrErrorImpl<>(successOrErrorDelegate);
  }

  private class GroovySuccessImpl<T> implements GroovySuccess<T> {

    private final Success<T> successDelegate;

    private GroovySuccessImpl(Success<T> successDelegate) {
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

  private class GroovySuccessOrErrorImpl<T> extends GroovySuccessImpl<T> implements GroovySuccessOrError<T> {
    private final SuccessOrError<T> successOrErrorDelegate;

    public GroovySuccessOrErrorImpl(SuccessOrError<T> successOrErrorDelegate) {
      super(successOrErrorDelegate);
      this.successOrErrorDelegate = successOrErrorDelegate;
    }

    @Override
    public GroovySuccess<T> onError(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
      return new GroovySuccessImpl<>(successOrErrorDelegate.onError(Util.action(closure)));
    }

    @Override
    public Success<T> onError(Action<? super Throwable> errorHandler) {
      return successOrErrorDelegate.onError(errorHandler);
    }
  }
}
