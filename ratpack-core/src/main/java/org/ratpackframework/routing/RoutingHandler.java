package org.ratpackframework.routing;

import org.ratpackframework.Action;
import org.ratpackframework.http.Exchange;
import org.ratpackframework.http.Handler;

import java.util.LinkedList;
import java.util.List;

public class RoutingHandler implements Handler {

  private final Action<? super Routing> action;

  public RoutingHandler(Action<? super Routing> action) {
    this.action = action;
  }

  @Override
  public void handle(Exchange exchange) {
    List<Handler> handlers = new LinkedList<>();
    Routing routing = new DefaultRouting(exchange, handlers);
    action.execute(routing);
    exchange.next(handlers);
  }

}
