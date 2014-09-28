
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
import ratpack.func.Action;
import ratpack.guice.BindingsSpec;
import ratpack.guice.NoSuchModuleException;
import ratpack.launch.LaunchConfig;

import javax.inject.Provider;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

class DefaultBindingsSpec implements BindingsSpec {

  private final List<Module> modules;
  private final LaunchConfig launchConfig;
  private final List<Action<? super Binder>> binderActions;
  private final List<Action<? super Injector>> injectorActions;

  public DefaultBindingsSpec(LaunchConfig launchConfig, List<Action<? super Binder>> binderActions, List<Action<? super Injector>> injectorActions, List<Module> modules) {
    this.launchConfig = launchConfig;
    this.binderActions = binderActions;
    this.injectorActions = injectorActions;
    this.modules = modules;
  }

  public LaunchConfig getLaunchConfig() {
    return launchConfig;
  }

  @Override
  public BindingsSpec bind(final Class<?> type) {
    return bindings(binder -> binder.bind(type));
  }

  @Override
  public <T> BindingsSpec bind(final Class<T> publicType, final Class<? extends T> implType) {
    return bindings(binder -> binder.bind(publicType).to(implType));
  }

  @Override
  public <T> BindingsSpec bind(final Class<? super T> publicType, final T instance) {
    return bindings(binder -> binder.bind(publicType).toInstance(instance));
  }

  @Override
  public <T> BindingsSpec bind(final T instance) {
    @SuppressWarnings("unchecked") final
    Class<T> type = (Class<T>) instance.getClass();
    return bindings(binder -> binder.bind(type).toInstance(instance));
  }

  @Override
  public <T> BindingsSpec provider(final Class<T> publicType, final Class<? extends Provider<? extends T>> providerType) {
    return bindings(binder -> binder.bind(publicType).toProvider(providerType));
  }

  @Override
  public BindingsSpec add(Module... modules) {
    Collections.addAll(this.modules, modules);
    return this;
  }

  @Override
  public BindingsSpec add(Iterable<? extends Module> modules) {
    for (Module module : modules) {
      this.modules.add(module);
    }
    return this;
  }

  @Override
  public <T extends Module> BindingsSpec config(Class<T> moduleClass, Consumer<? super T> configurer) throws NoSuchModuleException {
    for (Module module : modules) {
      if (moduleClass.isInstance(module)) {
        T cast = moduleClass.cast(module);
        configurer.accept(cast);
      }
    }

    return this;
  }

  @Override
  public BindingsSpec bindings(Action<? super Binder> action) {
    binderActions.add(action);
    return this;
  }

  @Override
  public BindingsSpec init(Action<Injector> action) {
    injectorActions.add(action);
    return this;
  }

  @Override
  public BindingsSpec init(final Class<? extends Runnable> clazz) {
    injectorActions.add(injector -> injector.getInstance(clazz).run());
    return this;
  }

}
