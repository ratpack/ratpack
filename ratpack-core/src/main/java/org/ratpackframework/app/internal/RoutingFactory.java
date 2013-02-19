package org.ratpackframework.app.internal;

import org.ratpackframework.handler.Handler;
import org.ratpackframework.app.Routing;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import java.util.List;

public interface RoutingFactory {
  Routing create(List<Handler<Routed<HttpExchange>>> routes);
}
