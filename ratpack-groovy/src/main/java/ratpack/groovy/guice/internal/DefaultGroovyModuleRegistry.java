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
import ratpack.api.Nullable;
import ratpack.groovy.guice.GroovyModuleRegistry;
import ratpack.groovy.internal.ClosureInvoker;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.guice.ModuleRegistry;
import ratpack.guice.internal.InjectorBackedRegistry;
import ratpack.launch.LaunchConfig;
import ratpack.registry.NotInRegistryException;
import ratpack.func.Action;

import javax.inject.Provider;
import java.util.List;

public class DefaultGroovyModuleRegistry implements GroovyModuleRegistry {

  private final ModuleRegistry moduleRegistry;

  public DefaultGroovyModuleRegistry(ModuleRegistry moduleRegistry) {
    this.moduleRegistry = moduleRegistry;
  }

  @Override
  public <T extends Module> void config(Class<T> moduleType, Closure<?> closure) {
    ClosureUtil.configureDelegateFirst(moduleRegistry.get(moduleType), closure);
  }

  @Override
  public void init(final Closure<?> closure) {
    doInit(closure, Void.class, Closure.OWNER_ONLY);
  }

  @Override
  public <T> void init(Class<T> clazz, Closure<?> closure) {
    doInit(closure, clazz, Closure.DELEGATE_FIRST);
  }

  private <T, N> void doInit(final Closure<T> closure, final Class<N> clazz, final int resolveStrategy) {
    init(new Action<Injector>() {
      @Override
      public void execute(Injector injector) throws Exception {
        InjectorBackedRegistry injectorBackedRegistry = new InjectorBackedRegistry(injector);
        N delegate = clazz.equals(Void.class) ? null : injector.getInstance(clazz);
        new ClosureInvoker<T, N>(closure).invoke(injectorBackedRegistry, delegate, resolveStrategy);
      }
    });
  }

  @Override
  public LaunchConfig getLaunchConfig() {
    return moduleRegistry.getLaunchConfig();
  }

  @Override
  public void bind(Class<?> type) {
    moduleRegistry.bind(type);
  }

  @Override
  public <T> void bind(Class<T> publicType, Class<? extends T> implType) {
    moduleRegistry.bind(publicType, implType);
  }

  @Override
  public <T> void bind(Class<? super T> publicType, T instance) {
    moduleRegistry.bind(publicType, instance);
  }

  @Override
  public <T> void bind(T instance) {
    moduleRegistry.bind(instance);
  }

  @Override
  public <T> void provider(Class<T> publicType, Class<? extends Provider<? extends T>> providerType) {
    moduleRegistry.provider(publicType, providerType);
  }

  @Override
  public void init(Action<Injector> action) {
    moduleRegistry.init(action);
  }

  @Override
  public void init(Class<? extends Runnable> clazz) {
    moduleRegistry.init(clazz);
  }

  public <O extends Module> void register(Class<O> type, O object) {
    moduleRegistry.register(type, object);
  }

  @Override
  public void register(Module object) {
    moduleRegistry.register(object);
  }

  public <O extends Module> O remove(Class<O> type) throws NotInRegistryException {
    return moduleRegistry.remove(type);
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    return moduleRegistry.get(type);
  }

  @Override
  @Nullable
  public <O> O maybeGet(Class<O> type) {
    return moduleRegistry.maybeGet(type);
  }

  @Override
  public <O> List<O> getAll(Class<O> type) {
    return moduleRegistry.getAll(type);
  }

}
