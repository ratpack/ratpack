package org.ratpackframework.groovy.app.internal

import groovy.transform.CompileStatic
import org.ratpackframework.groovy.Ratpack

@CompileStatic
class DefaultRatpack extends Script implements Ratpack {

  private Closure<?> modulesConfigurer;
  private Closure<?> routingConfigurer;

  @Override
  void modules(Closure<?> modulesConfigurer) {
    this.modulesConfigurer = modulesConfigurer;
  }

  @Override
  void routing(Closure<?> routingConfigurer) {
    this.routingConfigurer = routingConfigurer
  }

  Closure<?> getModulesConfigurer() {
    return modulesConfigurer
  }

  Closure<?> getRoutingConfigurer() {
    return routingConfigurer
  }

  @Override
  Object run() {
    this
  }

}
