package org.ratpackframework.error.internal;

import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

public class ErrorHandler implements Handler {

  private final Handler handler;

  public ErrorHandler(Handler handler) {
    this.handler = handler;
  }

  @Override
  public void handle(Exchange exchange) {
    try {
      handler.handle(exchange);
    } catch (Exception exception) {
      exchange.error(exception);
    }
  }
}
