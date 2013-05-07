package org.ratpackframework.guice;

import com.google.inject.Injector;
import org.ratpackframework.http.Exchange;
import org.ratpackframework.http.Handler;

public class InjectorBindingHandler implements Handler {

  private final Injector injector;
  private final Handler delegate;

  public InjectorBindingHandler(Injector injector, Handler delegate) {
    this.injector = injector;
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    exchange.nextWithContext(injector, delegate);
  }
}
