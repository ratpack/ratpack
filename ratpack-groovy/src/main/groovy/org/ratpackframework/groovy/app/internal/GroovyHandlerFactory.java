package org.ratpackframework.groovy.app.internal;

import org.ratpackframework.guice.DefaultGuiceBackedHandlerFactory;
import org.ratpackframework.guice.ModuleRegistry;
import org.ratpackframework.guice.RequestScopeModule;
import org.ratpackframework.session.SessionModule;

public class GroovyHandlerFactory extends DefaultGuiceBackedHandlerFactory {


  void registerExtraModules(ModuleRegistry registry) {
    registry.register(new SessionModule());
    registry.register(new RequestScopeModule());
  }

}
