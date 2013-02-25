package org.ratpackframework.groovy.app;

import com.google.inject.AbstractModule;
import org.ratpackframework.groovy.app.internal.ScriptBackedRouter;

import static com.google.inject.name.Names.named;
import static org.ratpackframework.bootstrap.internal.RootModule.HTTP_HANDLER;
import static org.ratpackframework.bootstrap.internal.RootModule.MAIN_APP_HTTP_HANDLER;

public class RoutingScriptModule extends AbstractModule {

  private final RoutingConfig routingConfig;

  public RoutingScriptModule(RoutingConfig routingConfig) {
    this.routingConfig = routingConfig;
  }

  @Override
  protected void configure() {
    bind(RoutingConfig.class).toInstance(routingConfig);
    bind(HTTP_HANDLER).annotatedWith(named(MAIN_APP_HTTP_HANDLER)).to(ScriptBackedRouter.class);
  }

}
