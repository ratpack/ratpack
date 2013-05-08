package org.ratpackframework.groovy.internal;

import groovy.lang.Closure;
import org.ratpackframework.groovy.Ratpack;

public class DefaultRatpack implements Ratpack {

  private Closure<?> modulesConfigurer;
  private Closure<?> routingConfigurer;

  @Override
  public void modules(Closure<?> modulesConfigurer) {
    this.modulesConfigurer = modulesConfigurer;
  }

  @Override
  public void routing(Closure<?> routingConfigurer) {
    this.routingConfigurer = routingConfigurer;
  }

  public Closure<?> getModulesConfigurer() {
    return modulesConfigurer;
  }

  public Closure<?> getRoutingConfigurer() {
    return routingConfigurer;
  }

}
