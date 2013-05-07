package org.ratpackframework.routing.internal;

import org.ratpackframework.http.Exchange;
import org.ratpackframework.http.Handler;
import org.ratpackframework.routing.Routing;

import java.util.List;

public class DefaultRouting implements Routing {

  private final Exchange exchange;
  private final List<Handler> handlers;

  public DefaultRouting(Exchange exchange, List<Handler> handlers) {
    this.exchange = exchange;
    this.handlers = handlers;
  }

  @Override
  public Exchange getExchange() {
    return exchange;
  }

  @Override
  public void route(Handler handler) {
    handlers.add(handler);
  }

}
