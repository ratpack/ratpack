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
import ratpack.func.Block;
import ratpack.groovy.Groovy;
import ratpack.groovy.handling.GroovyByMethodSpec;
import ratpack.handling.ByMethodSpec;
import ratpack.handling.Handler;

public class DefaultGroovyByMethodSpec implements GroovyByMethodSpec {

  private final ByMethodSpec delegate;

  public DefaultGroovyByMethodSpec(ByMethodSpec delegate) {
    this.delegate = delegate;
  }

  private GroovyByMethodSpec r(@SuppressWarnings("unused") Object ignore) {
    return this;
  }

  @Override
  public GroovyByMethodSpec get(Block block) {
    return r(delegate.get(block));
  }

  @Override
  public GroovyByMethodSpec get(Class<? extends Handler> clazz) {
    return r(delegate.get(clazz));
  }

  @Override
  public GroovyByMethodSpec get(Handler handler) {
    return r(delegate.get(handler));
  }

  @Override
  public GroovyByMethodSpec post(Block block) {
    return r(delegate.post(block));
  }

  @Override
  public GroovyByMethodSpec post(Class<? extends Handler> clazz) {
    return r(delegate.post(clazz));
  }

  @Override
  public GroovyByMethodSpec post(Handler handler) {
    return r(delegate.post(handler));
  }

  @Override
  public GroovyByMethodSpec put(Block block) {
    return r(delegate.put(block));
  }

  @Override
  public GroovyByMethodSpec put(Class<? extends Handler> clazz) {
    return r(delegate.put(clazz));
  }

  @Override
  public GroovyByMethodSpec put(Handler handler) {
    return r(delegate.put(handler));
  }

  @Override
  public GroovyByMethodSpec patch(Block block) {
    return r(delegate.patch(block));
  }

  @Override
  public GroovyByMethodSpec patch(Class<? extends Handler> clazz) {
    return r(delegate.patch(clazz));
  }

  @Override
  public GroovyByMethodSpec patch(Handler handler) {
    return r(delegate.patch(handler));
  }

  @Override
  public GroovyByMethodSpec options(Block block) {
    return r(delegate.options(block));
  }

  @Override
  public GroovyByMethodSpec options(Class<? extends Handler> clazz) {
    return r(delegate.options(clazz));
  }

  @Override
  public GroovyByMethodSpec options(Handler handler) {
    return r(delegate.options(handler));
  }

  @Override
  public GroovyByMethodSpec delete(Block block) {
    return r(delegate.delete(block));
  }

  @Override
  public GroovyByMethodSpec delete(Class<? extends Handler> clazz) {
    return r(delegate.delete(clazz));
  }

  @Override
  public GroovyByMethodSpec delete(Handler handler) {
    return r(delegate.delete(handler));
  }

  @Override
  public GroovyByMethodSpec named(String methodName, Block block) {
    return r(delegate.named(methodName, block));
  }

  @Override
  public GroovyByMethodSpec named(String methodName, Class<? extends Handler> clazz) {
    return r(delegate.named(methodName, clazz));
  }

  @Override
  public GroovyByMethodSpec named(String methodName, Handler handler) {
    return r(delegate.named(methodName, handler));
  }

  @Override
  public GroovyByMethodSpec get(Closure<?> closure) {
    return get(Groovy.groovyHandler(closure));
  }

  @Override
  public GroovyByMethodSpec post(Closure<?> closure) {
    return post(Groovy.groovyHandler(closure));
  }

  @Override
  public GroovyByMethodSpec put(Closure<?> closure) {
    return put(Groovy.groovyHandler(closure));
  }

  @Override
  public GroovyByMethodSpec patch(Closure<?> closure) {
    return patch(Groovy.groovyHandler(closure));
  }

  @Override
  public GroovyByMethodSpec options(Closure<?> closure) {
    return options(Groovy.groovyHandler(closure));
  }

  @Override
  public GroovyByMethodSpec delete(Closure<?> closure) {
    return delete(Groovy.groovyHandler(closure));
  }

  @Override
  public GroovyByMethodSpec named(String methodName, Closure<?> closure) {
    return named(methodName, Groovy.groovyHandler(closure));
  }

}
