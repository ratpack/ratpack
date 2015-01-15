/*
 * Copyright 2014 the original author or authors.
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

package ratpack.guice.internal;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import ratpack.func.Action;
import ratpack.guice.BindingsSpec;
import ratpack.guice.ConfigurableModule;
import ratpack.guice.NoSuchModuleException;
import ratpack.server.ServerConfig;

import java.util.List;
import java.util.function.Consumer;

public class DefaultBindingsSpec implements BindingsSpec {

  private final List<Module> modules;
  private final ServerConfig serverConfig;
  private final List<Action<? super Binder>> binderActions;
  private final List<Action<? super Injector>> injectorActions;

  public DefaultBindingsSpec(ServerConfig serverConfig, List<Action<? super Binder>> binderActions, List<Action<? super Injector>> injectorActions, List<Module> modules) {
    this.serverConfig = serverConfig;
    this.binderActions = binderActions;
    this.injectorActions = injectorActions;
    this.modules = modules;
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  @Override
  public BindingsSpec bind(final Class<?> type) {
    return binder(binder -> binder.bind(type));
  }

  @Override
  public <T> BindingsSpec bind(final Class<T> publicType, final Class<? extends T> implType) {
    return binder(binder -> binder.bind(publicType).to(implType));
  }

  @Override
  public <T> BindingsSpec bindInstance(final Class<? super T> publicType, final T instance) {
    return binder(binder -> binder.bind(publicType).toInstance(instance));
  }

  @Override
  public <T> BindingsSpec bindInstance(final T instance) {
    @SuppressWarnings("unchecked") final
    Class<T> type = (Class<T>) instance.getClass();
    return binder(binder -> binder.bind(type).toInstance(instance));
  }

  @Override
  public <T> BindingsSpec providerType(final Class<T> publicType, final Class<? extends Provider<? extends T>> providerType) {
    return binder(binder -> binder.bind(publicType).toProvider(providerType));
  }

  @Override
  public <T> BindingsSpec provider(Class<T> publicType, Provider<? extends T> provider) {
    return binder(b -> b.bind(publicType).toProvider(provider));
  }

  @Override
  public BindingsSpec add(Module module) {
    this.modules.add(module);
    return this;
  }

  public BindingsSpec add(Class<? extends Module> moduleClass) {
    return add(createModule(moduleClass));
  }

  private <T extends Module> T createModule(Class<T> clazz) {
    try {
      return clazz.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Module " + clazz.getName() + " is not reflectively instantiable", e);
    }
  }

  @Override
  public <C, T extends ConfigurableModule<C>> BindingsSpec add(Class<T> moduleClass, Action<? super C> configuration) {
    T t = createModule(moduleClass);
    t.configure(configuration);
    this.modules.add(t);
    return this;
  }

  @Override
  public <T extends Module> BindingsSpec config(Class<T> moduleClass, Consumer<? super T> configurer) throws NoSuchModuleException {
    modules.stream().filter(moduleClass::isInstance).map(moduleClass::cast).forEach(configurer);
    return this;
  }

  @Override
  public BindingsSpec binder(Action<? super Binder> action) {
    binderActions.add(action);
    return this;
  }

  @Override
  public BindingsSpec init(Action<? super Injector> action) {
    injectorActions.add(action);
    return this;
  }

  @Override
  public BindingsSpec init(final Class<? extends Runnable> clazz) {
    injectorActions.add(injector -> injector.getInstance(clazz).run());
    return this;
  }

}
