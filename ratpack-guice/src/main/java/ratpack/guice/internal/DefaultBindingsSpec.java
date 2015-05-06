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
import com.google.inject.Module;
import ratpack.func.Action;
import ratpack.guice.BindingsSpec;
import ratpack.guice.ConfigurableModule;
import ratpack.server.ServerConfig;

import java.util.List;

public class DefaultBindingsSpec implements BindingsSpec {

  private final List<Module> modules;
  private final ServerConfig serverConfig;
  private final List<Action<? super Binder>> binderActions;

  public DefaultBindingsSpec(ServerConfig serverConfig, List<Action<? super Binder>> binderActions, List<Module> modules) {
    this.serverConfig = serverConfig;
    this.binderActions = binderActions;
    this.modules = modules;
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  @Override
  public BindingsSpec module(Module module) {
    this.modules.add(module);
    return this;
  }

  public BindingsSpec module(Class<? extends Module> moduleClass) {
    return module(createModule(moduleClass));
  }

  private <T extends Module> T createModule(Class<T> clazz) {
    try {
      return clazz.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Module " + clazz.getName() + " is not reflectively instantiable", e);
    }
  }

  @Override
  public <C> BindingsSpec module(ConfigurableModule<C> module, Action<? super C> configurer) {
    module.configure(configurer);
    return module(module);
  }

  @Override
  public <C, T extends ConfigurableModule<C>> BindingsSpec module(Class<T> moduleClass, Action<? super C> configurer) {
    T t = createModule(moduleClass);
    return module(t, configurer);
  }

  @Override
  public <C> BindingsSpec moduleConfig(ConfigurableModule<C> module, C config, Action<? super C> configurer) {
    module.setConfig(config);
    return module(module, configurer);
  }

  @Override
  public <C, T extends ConfigurableModule<C>> BindingsSpec moduleConfig(Class<T> moduleClass, C config, Action<? super C> configurer) {
    T t = createModule(moduleClass);
    return moduleConfig(t, config, configurer);
  }

  @Override
  public BindingsSpec binder(Action<? super Binder> action) {
    binderActions.add(action);
    return this;
  }

}
