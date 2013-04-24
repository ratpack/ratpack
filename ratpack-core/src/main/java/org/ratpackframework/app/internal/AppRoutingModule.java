package org.ratpackframework.app.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import org.ratpackframework.Action;
import org.ratpackframework.app.Routing;
import org.ratpackframework.bootstrap.internal.RootModule;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import javax.inject.Named;

public class AppRoutingModule extends AbstractModule {

  public static final String MAIN_ROUTING_HANDLER = "mainRoutingHandler";
  public static final TypeLiteral<Action<Routing>> ROUTING_HANDLER = new TypeLiteral<Action<Routing>>() {
  };

  private final Action<LinkedBindingBuilder<Action<Routing>>> routingHandlerBindingHandler;

  public static AppRoutingModule create(final Class<? extends Action<Routing>> routingHandlerClass) {
    return new AppRoutingModule(new Action<LinkedBindingBuilder<Action<Routing>>>() {
      public void execute(LinkedBindingBuilder<Action<Routing>> bindingBuilder) {
        bindingBuilder.to(routingHandlerClass);
      }
    });
  }

  public static AppRoutingModule create(final Action<Routing> routingHandler) {
    return new AppRoutingModule(new Action<LinkedBindingBuilder<Action<Routing>>>() {
      public void execute(LinkedBindingBuilder<Action<Routing>> bindingBuilder) {
        bindingBuilder.toInstance(routingHandler);
      }
    });
  }

  public AppRoutingModule(Action<LinkedBindingBuilder<Action<Routing>>> routingHandlerBindingHandler) {
    this.routingHandlerBindingHandler = routingHandlerBindingHandler;
  }

  @Override
  protected void configure() {
    LinkedBindingBuilder<Action<Routing>> bindingBuilder = bind(ROUTING_HANDLER).annotatedWith(Names.named(MAIN_ROUTING_HANDLER));
    routingHandlerBindingHandler.execute(bindingBuilder);
  }

  @Provides
  @Named(RootModule.MAIN_APP_HTTP_HANDLER)
  Action<Routed<HttpExchange>> createHandler(RoutingConverter converter, @Named(MAIN_ROUTING_HANDLER) Action<Routing> routingHandler) {
    return converter.build(routingHandler);
  }

}
