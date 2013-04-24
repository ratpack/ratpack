package org.ratpackframework.bootstrap.internal;

import com.google.inject.Module;
import org.ratpackframework.bootstrap.ModuleRegistry;
import org.ratpackframework.Action;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultModuleRegistry implements ModuleRegistry {

  private final Map<Class<?>, Module> modules = new LinkedHashMap<>();

  @Override
  public void register(Module module) {
    Class<? extends Module> type = module.getClass();
    if (modules.containsKey(type)) {
      Object existing = modules.get(type);
      throw new IllegalArgumentException(String.format("Mdule '%s' is already registered with type '%s' (attempting to register '%s')", existing, type, module));
    }

    modules.put(type, module);
  }

  @Override
  public <T> T get(Class<T> moduleType) {
    Object configObject = modules.get(moduleType);
    if (configObject == null) {
      throw new IllegalArgumentException(String.format("No module registered with type '%s'", moduleType));
    }

    return moduleType.cast(configObject);
  }

  @Override
  public <T> T get(Class<T> moduleType, Action<? super T> configurer) {
    T configObject = get(moduleType);
    configurer.execute(configObject);
    return configObject;
  }

  @Override
  public <T extends Module> T remove(Class<T> moduleType) {
    if (!modules.containsKey(moduleType)) {
      throw new IllegalArgumentException(String.format("There is no module with type '%s'", moduleType));
    }

    return moduleType.cast(modules.remove(moduleType));
  }

  @Override
  public List<Module> getModules() {
    return new ArrayList<>(modules.values());
  }
}
