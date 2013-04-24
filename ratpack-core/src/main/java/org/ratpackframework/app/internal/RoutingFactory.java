package org.ratpackframework.app.internal;

import org.ratpackframework.Action;
import org.ratpackframework.app.Routing;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import java.util.List;

public interface RoutingFactory {
  Routing create(List<Action<Routed<HttpExchange>>> routes);
}
