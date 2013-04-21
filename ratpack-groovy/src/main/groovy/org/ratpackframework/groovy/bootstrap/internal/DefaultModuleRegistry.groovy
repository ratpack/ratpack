package org.ratpackframework.groovy.bootstrap.internal

import groovy.transform.CompileStatic
import org.ratpackframework.groovy.Closures
import org.ratpackframework.groovy.bootstrap.ModuleRegistry

@CompileStatic
public class DefaultModuleRegistry extends org.ratpackframework.bootstrap.internal.DefaultModuleRegistry implements ModuleRegistry {

  public <T> T get(Class<T> moduleType, Closure<?> configurer) {
    return super.get(moduleType, Closures.handler(moduleType, configurer))
  }

}
