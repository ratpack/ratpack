package org.ratpackframework.service.internal;

import org.ratpackframework.service.ServiceRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultServiceRegistry implements ServiceRegistry {

  private final Map<String, Object> store;

  public DefaultServiceRegistry(Map<String, Object> store) {
    this.store = new HashMap<>(store);
  }

  @Override
  public <T> T get(Class<T> type) {
    List<T> services = allOfType(type);
    if (services.isEmpty()) {
      return null;
    }
    if (services.size() == 1) {
      return services.get(0);
    } else {
      throw new IllegalArgumentException("There are multiple services of type " + type.getName());
    }
  }

  private <T> List<T> allOfType(Class<T> type) {
    List<T> services = new ArrayList<>(1);
    for (Object service : store.values()) {
      if (type.isInstance(service)) {
        services.add(type.cast(service));
      }
    }
    return services;
  }

  @Override
  public Object get(String name) {
    return store.get(name);
  }

  @Override
  public <T> T get(String name, Class<T> type) {
    Object service = get(name);
    if (service == null) {
      return null;
    } else if (type.isInstance(service)) {
      return type.cast(service);
    } else {
      throw new IllegalArgumentException(String.format("Service '%s' of type '%s' is not compatible with target type '%s'", name, service.getClass().getName(), type.getName()));
    }
  }

}
