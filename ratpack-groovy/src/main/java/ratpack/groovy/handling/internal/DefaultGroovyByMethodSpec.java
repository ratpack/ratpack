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

package ratpack.groovy.handling.internal;

import groovy.lang.Closure;
import io.netty.handler.codec.http.HttpMethod;
import ratpack.func.Block;
import ratpack.groovy.handling.GroovyByMethodSpec;
import ratpack.handling.ByMethodSpec;
import ratpack.handling.Context;
import ratpack.handling.Handler;

public class DefaultGroovyByMethodSpec implements GroovyByMethodSpec {

  private final ByMethodSpec delegate;
  private final Context context;

  public DefaultGroovyByMethodSpec(ByMethodSpec delegate, Context context) {
    this.delegate = delegate;
    this.context = context;
  }

  @Override
  public ByMethodSpec get(Block block) {
    return delegate.get(block);
  }

  @Override
  public ByMethodSpec get(Class<? extends Handler> clazz) {
    return delegate.get(clazz);
  }

  @Override
  public ByMethodSpec get(Handler handler) {
    return delegate.get(handler);
  }

  @Override
  public ByMethodSpec get(Closure<?> closure) {
   return named(HttpMethod.GET.name(), closure);
  }

  @Override
  public ByMethodSpec post(Block block) {
    return delegate.post(block);
  }

  @Override
  public ByMethodSpec post(Class<? extends Handler> clazz) {
    return delegate.post(clazz);
  }

  @Override
  public ByMethodSpec post(Handler handler) {
    return delegate.post(handler);
  }

  @Override
  public ByMethodSpec post(Closure<?> closure) {
    return named(HttpMethod.POST.name(), closure);
  }

  @Override
  public ByMethodSpec put(Block block) {
    return delegate.put(block);
  }

  @Override
  public ByMethodSpec put(Class<? extends Handler> clazz) {
    return delegate.put(clazz);
  }

  @Override
  public ByMethodSpec put(Handler handler) {
    return delegate.put(handler);
  }

  @Override
  public ByMethodSpec put(Closure<?> closure) {
    return named(HttpMethod.PUT.name(), closure);
  }

  @Override
  public ByMethodSpec patch(Block block) {
    return delegate.patch(block);
  }

  @Override
  public ByMethodSpec patch(Class<? extends Handler> clazz) {
    return delegate.patch(clazz);
  }

  @Override
  public ByMethodSpec patch(Handler handler) {
    return delegate.patch(handler);
  }

  @Override
  public ByMethodSpec patch(Closure<?> closure) {
    return named(HttpMethod.PATCH.name(), closure);
  }

  @Override
  public ByMethodSpec options(Block block) {
    return delegate.options(block);
  }

  @Override
  public ByMethodSpec options(Class<? extends Handler> clazz) {
    return delegate.options(clazz);
  }

  @Override
  public ByMethodSpec options(Handler handler) {
    return delegate.options(handler);
  }

  @Override
  public ByMethodSpec options(Closure<?> closure) {
    return named(HttpMethod.OPTIONS.name(), closure);
  }

  @Override
  public ByMethodSpec delete(Block block) {
    return delegate.delete(block);
  }

  @Override
  public ByMethodSpec delete(Class<? extends Handler> clazz) {
    return delegate.delete(clazz);
  }

  @Override
  public ByMethodSpec delete(Handler handler) {
    return delegate.delete(handler);
  }

  @Override
  public ByMethodSpec delete(Closure<?> closure) {
    return named(HttpMethod.DELETE.name(), closure);
  }

  @Override
  public ByMethodSpec named(String methodName, Block block) {
    return delegate.named(methodName, block);
  }

  @Override
  public ByMethodSpec named(String methodName, Class<? extends Handler> clazz) {
    return delegate.named(methodName, clazz);
  }

  @Override
  public ByMethodSpec named(String methodName, Handler handler) {
    return delegate.named(methodName, handler);
  }

  @Override
  public ByMethodSpec named(String methodName, Closure<?> closure) {
    closure.setDelegate(context);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    if (closure.getMaximumNumberOfParameters() == 0) {
      delegate.named(methodName, (Block) closure::call);
    } else {
      delegate.named(methodName, (Handler) closure::call);
    }
    return this;
  }

}
