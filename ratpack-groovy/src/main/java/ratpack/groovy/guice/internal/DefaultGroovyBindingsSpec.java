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

import com.google.inject.Injector;
import com.google.inject.Module;
import groovy.lang.Closure;
import ratpack.func.Action;
import ratpack.groovy.guice.GroovyBindingsSpec;
import ratpack.groovy.internal.ClosureInvoker;
import ratpack.guice.BindingsSpec;
import ratpack.guice.Guice;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;

import javax.inject.Provider;

public class DefaultGroovyBindingsSpec implements GroovyBindingsSpec {

  private final BindingsSpec bindingsSpec;

  public DefaultGroovyBindingsSpec(BindingsSpec bindingsSpec) {
    this.bindingsSpec = bindingsSpec;
  }

  @Override
  public void init(final Closure<?> closure) {
    doInit(closure, Void.class, Closure.OWNER_ONLY);
  }

  private <T, N> void doInit(final Closure<T> closure, final Class<N> clazz, final int resolveStrategy) {
    init(new Action<Injector>() {
      @Override
      public void execute(Injector injector) throws Exception {
        Registry injectorBackedRegistry = Guice.registry(injector);
        N delegate = clazz.equals(Void.class) ? null : injector.getInstance(clazz);
        new ClosureInvoker<T, N>(closure).invoke(injectorBackedRegistry, delegate, resolveStrategy);
      }
    });
  }

  @Override
  public LaunchConfig getLaunchConfig() {
    return bindingsSpec.getLaunchConfig();
  }

  @Override
  public void bind(Class<?> type) {
    bindingsSpec.bind(type);
  }

  @Override
  public <T> void bind(Class<T> publicType, Class<? extends T> implType) {
    bindingsSpec.bind(publicType, implType);
  }

  @Override
  public <T> void bind(Class<? super T> publicType, T instance) {
    bindingsSpec.bind(publicType, instance);
  }

  @Override
  public <T> void bind(T instance) {
    bindingsSpec.bind(instance);
  }

  @Override
  public <T> void provider(Class<T> publicType, Class<? extends Provider<? extends T>> providerType) {
    bindingsSpec.provider(publicType, providerType);
  }

  @Override
  public void init(Action<Injector> action) {
    bindingsSpec.init(action);
  }

  @Override
  public void init(Class<? extends Runnable> clazz) {
    bindingsSpec.init(clazz);
  }

  @Override
  public <T extends Module> T config(Class<T> moduleClass) {
    return bindingsSpec.config(moduleClass);
  }

  @Override
  public void add(Module... modules) {
    bindingsSpec.add(modules);
  }

  @Override
  public void add(Iterable<? extends Module> modules) {
    bindingsSpec.add(modules);
  }
}
