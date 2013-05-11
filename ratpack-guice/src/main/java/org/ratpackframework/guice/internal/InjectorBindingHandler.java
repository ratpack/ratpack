package org.ratpackframework.guice.internal;

import com.google.inject.Injector;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

public class InjectorBindingHandler implements Handler {

  private final Injector injector;
  private final Handler delegate;

  public InjectorBindingHandler(Injector injector, Handler delegate) {
    this.injector = injector;
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    exchange.nextWithContext(new InjectorBackedContext(exchange.getContext(), injector), delegate);
  }
}
