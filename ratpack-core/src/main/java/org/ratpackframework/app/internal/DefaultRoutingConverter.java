package org.ratpackframework.app.internal;

import org.ratpackframework.app.Routing;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.CompositeRouter;
import org.ratpackframework.routing.Routed;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

public class DefaultRoutingConverter implements RoutingConverter {

  private final RoutingFactory routingFactory;

  @Inject
  public DefaultRoutingConverter(RoutingFactory routingFactory) {
    this.routingFactory = routingFactory;
  }

  @Override
  public Handler<Routed<HttpExchange>> build(Handler<Routing> routingHandler) {
    List<Handler<Routed<HttpExchange>>> routers = new LinkedList<>();
    Routing routing = routingFactory.create(routers);
    routingHandler.handle(routing);
    return new CompositeRouter<>(routers);
  }

}
