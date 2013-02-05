package org.ratpackframework.service;

public interface ServiceRegistry {

  <T> T get(Class<T> type);

  Object get(String name);

  <T> T get(String name, Class<T> type);
}
