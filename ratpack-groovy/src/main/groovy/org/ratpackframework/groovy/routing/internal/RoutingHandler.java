package org.ratpackframework.groovy.routing.internal;

import org.ratpackframework.Action;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.routing.*;

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
