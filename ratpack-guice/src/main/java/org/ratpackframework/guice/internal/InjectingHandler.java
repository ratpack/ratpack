package org.ratpackframework.guice.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

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
    // TODO what's the cost of creating an injector for every request
    //      if it's not negligible, we could support using an explicit injector instead of getting
    //      it from the request context.
    //
    //      Another option would be to cache the injector. If the injection context is the same, then just reuse.
    Injector injector = exchange.getContext().require(Injector.class);
    Injector childInjector = injector.createChildInjector(module);
    Handler instance = childInjector.getInstance(handlerType);
    instance.handle(exchange);
  }

}
