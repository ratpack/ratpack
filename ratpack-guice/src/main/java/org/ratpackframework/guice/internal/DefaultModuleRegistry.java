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

package org.ratpackframework.guice.internal;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Module;
import org.ratpackframework.guice.ModuleRegistry;
import org.ratpackframework.guice.NoSuchModuleException;
import org.ratpackframework.launch.LaunchConfig;
import org.ratpackframework.util.Action;

import javax.inject.Provider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultModuleRegistry implements ModuleRegistry {

  private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<Class<? extends Module>, Module>();
  private final LaunchConfig launchConfig;

  private final ImmutableList.Builder<Action<Binder>> actions = ImmutableList.builder();

  public DefaultModuleRegistry(LaunchConfig launchConfig) {
    this.launchConfig = launchConfig;
  }

  public LaunchConfig getLaunchConfig() {
    return launchConfig;
  }

  public void bind(final Class<?> type) {
    actions.add(new Action<Binder>() {
      public void execute(Binder binder) {
        binder.bind(type);
      }
    });
  }

  public <T> void bind(final Class<T> publicType, final Class<? extends T> implType) {
    actions.add(new Action<Binder>() {
      public void execute(Binder binder) {
        binder.bind(publicType).to(implType);
      }
    });
  }

  public <T> void provider(final Class<T> publicType, final Class<? extends Provider<? extends T>> providerType) {
    actions.add(new Action<Binder>() {
      public void execute(Binder binder) {
        binder.bind(publicType).toProvider(providerType);
      }
    });
  }

  @SuppressWarnings("unchecked")
  public void register(Module module) {
    register((Class<Module>) module.getClass(), module);
  }

  public <O extends Module> void register(Class<O> type, O module) {
    if (modules.containsKey(type)) {
      Object existing = modules.get(type);
      throw new IllegalArgumentException(String.format("Module '%s' is already registered with type '%s' (attempting to register '%s')", existing, type, module));
    }

    modules.put(type, module);
  }

  public <T extends Module> T get(Class<T> moduleType) {
    Module module = modules.get(moduleType);
    if (module == null) {
      throw new NoSuchModuleException(moduleType);
    }

    return moduleType.cast(module);
  }

  public <O extends Module> O maybeGet(Class<O> type) {
    Module module = modules.get(type);
    if (module == null) {
      return null;
    }

    return type.cast(module);
  }

  public <T extends Module> T remove(Class<T> moduleType) {
    if (!modules.containsKey(moduleType)) {
      throw new NoSuchModuleException(moduleType);
    }

    return moduleType.cast(modules.remove(moduleType));
  }

  public List<Module> getModules() {
    return ImmutableList.<Module>builder()
      .addAll(modules.values())
      .add(createOverrideModule())
      .build();
  }

  private Module createOverrideModule() {
    return new Module() {
      public void configure(Binder binder) {
        for (Action<Binder> binderAction : actions.build()) {
          binderAction.execute(binder);
        }
      }
    };
  }

}
