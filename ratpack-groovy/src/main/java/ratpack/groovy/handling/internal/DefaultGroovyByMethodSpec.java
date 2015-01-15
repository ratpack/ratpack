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
import ratpack.func.NoArgAction;
import ratpack.groovy.Groovy;
import ratpack.groovy.handling.GroovyByMethodSpec;
import ratpack.groovy.handling.GroovyContext;
import ratpack.handling.ByMethodSpec;
import ratpack.handling.Handler;

public class DefaultGroovyByMethodSpec implements GroovyByMethodSpec {

  private final ByMethodSpec delegate;

  public DefaultGroovyByMethodSpec(ByMethodSpec delegate) {
    this.delegate = delegate;
  }

  @Override
  public GroovyByMethodSpec get(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    get(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByMethodSpec post(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    post(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByMethodSpec put(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    put(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByMethodSpec patch(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    patch(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByMethodSpec delete(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    delete(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByMethodSpec named(String methodName, @DelegatesTo(GroovyContext.class) Closure<?> closure) {
    named(methodName, Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public ByMethodSpec get(NoArgAction handler) {
    return delegate.get(handler);
  }

  @Override
  public ByMethodSpec get(Handler handler) {
    return delegate.get(handler);
  }

  @Override
  public ByMethodSpec post(NoArgAction handler) {
    return delegate.post(handler);
  }

  @Override
  public ByMethodSpec post(Handler handler) {
    return delegate.post(handler);
  }

  @Override
  public ByMethodSpec put(NoArgAction handler) {
    return delegate.put(handler);
  }

  @Override
  public ByMethodSpec put(Handler handler) {
    return delegate.put(handler);
  }

  @Override
  public ByMethodSpec patch(NoArgAction handler) {
    return delegate.patch(handler);
  }

  @Override
  public ByMethodSpec patch(Handler handler) {
    return delegate.patch(handler);
  }

  @Override
  public ByMethodSpec delete(NoArgAction handler) {
    return delegate.delete(handler);
  }

  @Override
  public ByMethodSpec delete(Handler handler) {
    return delegate.delete(handler);
  }

  @Override
  public ByMethodSpec named(String methodName, NoArgAction handler) {
    return delegate.named(methodName, handler);
  }

  @Override
  public ByMethodSpec named(String methodName, Handler handler) {
    return delegate.named(methodName, handler);
  }

}
