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
import ratpack.groovy.handling.GroovyByContentHandler;
import ratpack.groovy.handling.GroovyContext;
import ratpack.handling.ByContentHandler;
import ratpack.handling.Context;

import static ratpack.groovy.internal.Util.delegateFirstRunnable;

public class DefaultGroovyByContentHandler implements GroovyByContentHandler {

  private final GroovyContext context;
  private final ByContentHandler delegate;

  public DefaultGroovyByContentHandler(GroovyContext context, ByContentHandler byContentHandler) {
    this.context = context;
    this.delegate = byContentHandler;
  }

  @Override
  public GroovyByContentHandler type(String mimeType, @DelegatesTo(GroovyContext.class) Closure<?> closure) {
    type(mimeType, delegateFirstRunnable(context, context, closure));
    return this;
  }

  @Override
  public GroovyByContentHandler plainText(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    plainText(delegateFirstRunnable(context, context, closure));
    return this;
  }

  @Override
  public GroovyByContentHandler html(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    html(delegateFirstRunnable(context, context, closure));
    return this;
  }

  @Override
  public GroovyByContentHandler json(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    json(delegateFirstRunnable(context, context, closure));
    return this;
  }

  @Override
  public GroovyByContentHandler xml(@DelegatesTo(GroovyContext.class) Closure<?> closure) {
    xml(delegateFirstRunnable(context, context, closure));
    return this;
  }

  @Override
  public ByContentHandler type(String mimeType, Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return type(mimeType, (Closure) runnable);
    }
    return delegate.type(mimeType, runnable);
  }

  @Override
  public ByContentHandler plainText(Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return plainText((Closure) runnable);
    }
    return delegate.plainText(runnable);
  }

  @Override
  public ByContentHandler html(Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return html((Closure) runnable);
    }
    return delegate.html(runnable);
  }

  @Override
  public ByContentHandler json(Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return json((Closure) runnable);
    }
    return delegate.json(runnable);
  }

  @Override
  public ByContentHandler xml(Runnable runnable) {
    if (Closure.class.isInstance(runnable)) {
      return xml((Closure) runnable);
    }
    return delegate.xml(runnable);
  }

  @Override
  public void handle(Context context) {
    delegate.handle(context);
  }
}
