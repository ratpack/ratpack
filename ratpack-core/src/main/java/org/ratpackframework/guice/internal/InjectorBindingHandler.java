package org.ratpackframework.guice.internal;

import com.google.inject.Injector;
import org.ratpackframework.guice.InjectionContext;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

public class InjectorBindingHandler implements Handler {

  private final InjectionContext context;
  private final Handler delegate;

  public InjectorBindingHandler(Injector injector, Handler delegate) {
    this.context = new DefaultInjectionContext(injector);
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    exchange.nextWithContext(context, delegate);
  }
}
