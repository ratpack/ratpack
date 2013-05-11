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

import com.google.inject.Module;
import org.ratpackframework.guice.ModuleRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultModuleRegistry implements ModuleRegistry {

  private final Map<Class<?>, Module> modules = new LinkedHashMap<Class<?>, Module>();

  public void register(Module module) {
    Class<? extends Module> type = module.getClass();
    if (modules.containsKey(type)) {
      Object existing = modules.get(type);
      throw new IllegalArgumentException(String.format("Mdule '%s' is already registered with type '%s' (attempting to register '%s')", existing, type, module));
    }

    modules.put(type, module);
  }

  public <T> T get(Class<T> moduleType) {
    Object configObject = modules.get(moduleType);
    if (configObject == null) {
      throw new IllegalArgumentException(String.format("No module registered with type '%s'", moduleType));
    }

    return moduleType.cast(configObject);
  }

  public <T extends Module> T remove(Class<T> moduleType) {
    if (!modules.containsKey(moduleType)) {
      throw new IllegalArgumentException(String.format("There is no module with type '%s'", moduleType));
    }

    return moduleType.cast(modules.remove(moduleType));
  }

  public List<Module> getModules() {
    return new ArrayList<Module>(modules.values());
  }
}
