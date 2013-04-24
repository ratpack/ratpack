package org.ratpackframework.app.internal;

import org.ratpackframework.app.Routing;
import org.ratpackframework.Action;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

public interface RoutingConverter {

  Action<Routed<HttpExchange>> build(Action<Routing> routingHandler);

}
