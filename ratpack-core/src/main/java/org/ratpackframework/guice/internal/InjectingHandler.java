package org.ratpackframework.guice.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.ratpackframework.guice.InjectionContext;
import org.ratpackframework.http.Exchange;
import org.ratpackframework.http.Handler;

public class InjectingHandler implements Handler {

  private final Class<? extends Handler> handlerType;
  private final AbstractModule module;

  public InjectingHandler(final Class<? extends Handler> handlerType) {
    this.handlerType = handlerType;
    module = new AbstractModule() {
      @Override
      protected void configure() {
        bind(handlerType);
      }
    };
  }

  @Override
  public void handle(Exchange exchange) {
    InjectionContext context = exchange.getContext().require(InjectionContext.class);
    Injector injector = context.getInjector();
    Injector childInjector = injector.createChildInjector(module);
    Handler instance = childInjector.getInstance(handlerType);
    instance.handle(exchange);
  }

}
