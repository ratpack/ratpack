package org.ratpackframework.guice.internal;

import com.google.inject.Injector;
import com.google.inject.Module;
import org.ratpackframework.guice.InjectionContext;

public class DefaultInjectionContext implements InjectionContext {

  private final Injector injector;

  public DefaultInjectionContext(Injector injector) {
    this.injector = injector;
  }

  @Override
  public Injector getInjector() {
    return injector;
  }

}
