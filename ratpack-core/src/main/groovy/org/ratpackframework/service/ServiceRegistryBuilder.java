package org.ratpackframework.service;

import org.ratpackframework.service.internal.DefaultServiceRegistry;

import java.util.HashMap;
import java.util.Map;

public class ServiceRegistryBuilder {

  private final Map<String, Object> store = new HashMap<>(10);

  public void doAdd(String name, Object service) {
    if (store.containsKey(name)) {
      throw new IllegalArgumentException(String.format("Cannot add service %s as there is already a service with name '%s'", service, name));
    }

    store.put(name, service);
  }

  public ServiceRegistryBuilder add(String name, Object service) {
    doAdd(name, service);
    return this;
  }

  public ServiceRegistryBuilder add(Object service) {
    doAdd(decapitalize(service.getClass().getSimpleName()), service);
    return this;
  }

  public ServiceRegistry build() {
    return new DefaultServiceRegistry(store);
  }

  private String decapitalize(String str) {
    return str.substring(0, 1).toLowerCase() + str.substring(1);
  }

}
