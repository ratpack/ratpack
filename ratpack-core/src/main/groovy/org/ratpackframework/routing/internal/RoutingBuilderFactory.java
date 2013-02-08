package org.ratpackframework.routing.internal;

import org.ratpackframework.Handler;
import org.ratpackframework.Routing;
import org.ratpackframework.handler.HttpExchange;
import org.ratpackframework.routing.Routed;

import java.util.List;

public interface RoutingBuilderFactory {
  Routing create(List<Handler<Routed<HttpExchange>>> routes);
}
