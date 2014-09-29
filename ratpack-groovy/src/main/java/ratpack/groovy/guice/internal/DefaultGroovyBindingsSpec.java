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

package ratpack.groovy.guice.internal;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import groovy.lang.Closure;
import ratpack.func.Action;
import ratpack.groovy.guice.GroovyBindingsSpec;
import ratpack.groovy.internal.ClosureInvoker;
import ratpack.guice.BindingsSpec;
import ratpack.guice.Guice;
import ratpack.guice.NoSuchModuleException;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;

import java.util.function.Consumer;

public class DefaultGroovyBindingsSpec implements GroovyBindingsSpec {

  private final BindingsSpec delegate;

  public DefaultGroovyBindingsSpec(BindingsSpec delegate) {
    this.delegate = delegate;
  }

  @Override
  public GroovyBindingsSpec init(final Closure<?> closure) {
    doInit(closure, Void.class, Closure.OWNER_ONLY);
    return this;
  }

  private <T, N> void doInit(final Closure<T> closure, final Class<N> clazz, final int resolveStrategy) {
    init(injector -> {
      Registry injectorBackedRegistry = Guice.registry(injector);
      N delegate = clazz.equals(Void.class) ? null : injector.getInstance(clazz);
      new ClosureInvoker<T, N>(closure).invoke(injectorBackedRegistry, delegate, resolveStrategy);
    });
  }

  @Override
  public LaunchConfig getLaunchConfig() {
    return delegate.getLaunchConfig();
  }

  @Override
  public GroovyBindingsSpec bind(Class<?> type) {
    delegate.bind(type);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec bind(Class<T> publicType, Class<? extends T> implType) {
    delegate.bind(publicType, implType);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec bind(Class<? super T> publicType, T instance) {
    delegate.bind(publicType, instance);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec bind(T instance) {
    delegate.bind(instance);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec provider(Class<T> publicType, Class<? extends Provider<? extends T>> providerType) {
    delegate.provider(publicType, providerType);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec provider(Class<T> publicType, Provider<? extends T> provider) {
    delegate.provider(publicType, provider);
    return this;
  }

  @Override
  public GroovyBindingsSpec init(Action<? super Injector> action) {
    delegate.init(action);
    return this;
  }

  @Override
  public GroovyBindingsSpec init(Class<? extends Runnable> clazz) {
    delegate.init(clazz);
    return this;
  }

  @Override
  public <T extends Module> GroovyBindingsSpec config(Class<T> moduleClass, Consumer<? super T> configurer) throws NoSuchModuleException {
    delegate.config(moduleClass, configurer);
    return this;
  }

  @Override
  public GroovyBindingsSpec bindings(Action<? super Binder> action) {
    delegate.bindings(action);
    return this;
  }

  @Override
  public GroovyBindingsSpec add(Module... modules) {
    delegate.add(modules);
    return this;
  }

  @Override
  public GroovyBindingsSpec add(Iterable<? extends Module> modules) {
    delegate.add(modules);
    return this;
  }
}
