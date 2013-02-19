package org.ratpackframework.app.internal;

import org.ratpackframework.app.Routing;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

public interface RoutingConverter {

  Handler<Routed<HttpExchange>> build(Handler<Routing> routingHandler);

}
