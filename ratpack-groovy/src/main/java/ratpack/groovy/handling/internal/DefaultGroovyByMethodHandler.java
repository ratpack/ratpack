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

package ratpack.groovy.handling.internal;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.api.NonBlocking;
import ratpack.groovy.handling.GroovyByMethodHandler;
import ratpack.groovy.handling.GroovyContext;
import ratpack.handling.ByMethodHandler;
import ratpack.handling.Context;

import static ratpack.groovy.internal.ClosureUtil.delegateFirstRunnable;

public class DefaultGroovyByMethodHandler implements GroovyByMethodHandler {

  private final GroovyContext groovyContext;
  private final ByMethodHandler delegate;

  public DefaultGroovyByMethodHandler(GroovyContext groovyContext, ByMethodHandler delegate) {
    this.groovyContext = groovyContext;
    this.delegate = delegate;
  }

  @Override
  public GroovyByMethodHandler get(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    get(delegateFirstRunnable(groovyContext, groovyContext, closure));
    return this;
  }

  @Override
  public GroovyByMethodHandler post(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    post(delegateFirstRunnable(groovyContext, groovyContext, closure));
    return this;
  }

  @Override
  public GroovyByMethodHandler put(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    put(delegateFirstRunnable(groovyContext, groovyContext, closure));
    return this;
  }

  @Override
  public GroovyByMethodHandler delete(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    delete(delegateFirstRunnable(groovyContext, groovyContext, closure));
    return this;
  }

  @Override
  public GroovyByMethodHandler named(String methodName, @DelegatesTo(GroovyContext.class) Closure<?> closure) {
    named(methodName, delegateFirstRunnable(groovyContext, groovyContext, closure));
    return this;
  }

  @Override
  public ByMethodHandler get(Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return get((Closure<?>) runnable);
    }
    return delegate.get(runnable);
  }

  @Override
  public ByMethodHandler post(Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return post((Closure<?>) runnable);
    }
    return delegate.post(runnable);
  }

  @Override
  public ByMethodHandler put(Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return put((Closure<?>) runnable);
    }
    return delegate.put(runnable);
  }

  @Override
  public ByMethodHandler delete(Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return delete((Closure<?>) runnable);
    }
    return delegate.delete(runnable);
  }

  @Override
  public ByMethodHandler named(String methodName, Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return named(methodName, (Closure<?>) runnable);
    }
    return delegate.named(methodName, runnable);
  }

  @Override
  @NonBlocking
  public void handle(Context context) throws Exception {
    delegate.handle(context);
  }
}
