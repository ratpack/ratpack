package org.ratpackframework.groovy.bootstrap

interface ModuleRegistry extends org.ratpackframework.bootstrap.ModuleRegistry {

  public <T> T get(Class<T> moduleType, Closure<?> configurer)

}
