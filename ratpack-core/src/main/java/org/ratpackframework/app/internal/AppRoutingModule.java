package org.ratpackframework.app.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import org.ratpackframework.app.Routing;
import org.ratpackframework.bootstrap.internal.RootModule;
import org.ratpackframework.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import javax.inject.Named;

public class AppRoutingModule extends AbstractModule {

  public static final String MAIN_ROUTING_HANDLER = "mainRoutingHandler";
  public static final TypeLiteral<Handler<Routing>> ROUTING_HANDLER = new TypeLiteral<Handler<Routing>>() {
  };

  private final Handler<LinkedBindingBuilder<Handler<Routing>>> routingHandlerBindingHandler;

  public static AppRoutingModule create(final Class<? extends Handler<Routing>> routingHandlerClass) {
    return new AppRoutingModule(new Handler<LinkedBindingBuilder<Handler<Routing>>>() {
      public void handle(LinkedBindingBuilder<Handler<Routing>> bindingBuilder) {
        bindingBuilder.to(routingHandlerClass);
      }
    });
  }

  public static AppRoutingModule create(final Handler<Routing> routingHandler) {
    return new AppRoutingModule(new Handler<LinkedBindingBuilder<Handler<Routing>>>() {
      public void handle(LinkedBindingBuilder<Handler<Routing>> bindingBuilder) {
        bindingBuilder.toInstance(routingHandler);
      }
    });
  }

  public AppRoutingModule(Handler<LinkedBindingBuilder<Handler<Routing>>> routingHandlerBindingHandler) {
    this.routingHandlerBindingHandler = routingHandlerBindingHandler;
  }

  @Override
  protected void configure() {
    LinkedBindingBuilder<Handler<Routing>> bindingBuilder = bind(ROUTING_HANDLER).annotatedWith(Names.named(MAIN_ROUTING_HANDLER));
    routingHandlerBindingHandler.handle(bindingBuilder);
  }

  @Provides
  @Named(RootModule.MAIN_APP_HTTP_HANDLER)
  Handler<Routed<HttpExchange>> createHandler(RoutingConverter converter, @Named(MAIN_ROUTING_HANDLER) Handler<Routing> routingHandler) {
    return converter.build(routingHandler);
  }

}
