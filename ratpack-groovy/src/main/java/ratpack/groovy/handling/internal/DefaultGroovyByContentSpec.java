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
import ratpack.groovy.handling.GroovyByContentSpec;
import ratpack.groovy.handling.GroovyContext;
import ratpack.handling.ByContentSpec;
import ratpack.handling.Handler;

public class DefaultGroovyByContentSpec implements GroovyByContentSpec {

  private final ByContentSpec delegate;

  public DefaultGroovyByContentSpec(ByContentSpec delegate) {
    this.delegate = delegate;
  }

  @Override
  public GroovyByContentSpec type(String mimeType, @DelegatesTo(GroovyContext.class) Closure<?> closure) {
    type(mimeType, Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByContentSpec plainText(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    plainText(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByContentSpec html(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    html(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByContentSpec json(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    json(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByContentSpec xml(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    xml(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public GroovyByContentSpec noMatch(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    noMatch(Groovy.groovyHandler(closure));
    return this;
  }

  @Override
  public ByContentSpec type(String mimeType, NoArgAction handler) {
    return delegate.type(mimeType, handler);
  }

  @Override
  public ByContentSpec type(String mimeType, Handler handler) {
    return delegate.type(mimeType, handler);
  }

  @Override
  public ByContentSpec plainText(NoArgAction handler) {
    return delegate.plainText(handler);
  }

  @Override
  public ByContentSpec plainText(Handler handler) {
    return delegate.plainText(handler);
  }

  @Override
  public ByContentSpec html(NoArgAction handler) {
    return delegate.html(handler);
  }

  @Override
  public ByContentSpec html(Handler handler) {
    return delegate.html(handler);
  }

  @Override
  public ByContentSpec json(NoArgAction handler) {
    return delegate.json(handler);
  }

  @Override
  public ByContentSpec json(Handler handler) {
    return delegate.json(handler);
  }

  @Override
  public ByContentSpec xml(NoArgAction handler) {
    return delegate.xml(handler);
  }

  @Override
  public ByContentSpec xml(Handler handler) {
    return delegate.xml(handler);
  }

  @Override
  public ByContentSpec noMatch(NoArgAction handler) {
    return delegate.noMatch(handler);
  }

  @Override
  public ByContentSpec noMatch(Handler handler) {
    return delegate.noMatch(handler);
  }

  @Override
  public ByContentSpec noMatch(String mimeType) {
    return delegate.noMatch(mimeType);
  }

}
