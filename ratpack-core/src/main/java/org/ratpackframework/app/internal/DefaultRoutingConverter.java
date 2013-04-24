package org.ratpackframework.app.internal;

import org.ratpackframework.app.Routing;
import org.ratpackframework.Action;
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
  public Action<Routed<HttpExchange>> build(Action<Routing> routingHandler) {
    List<Action<Routed<HttpExchange>>> routers = new LinkedList<>();
    Routing routing = routingFactory.create(routers);
    routingHandler.execute(routing);
    return new CompositeRouter<>(routers);
  }

}
