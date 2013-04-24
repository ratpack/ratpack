package org.ratpackframework.error;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.Action;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

/**
 * Simply sends a HTTP 404 back to the client.
 */
public class DefaultNotFoundHandler implements Action<Routed<HttpExchange>> {

  @Override
  public void execute(Routed<HttpExchange> exchange) {
    exchange.get().end(HttpResponseStatus.NOT_FOUND);
  }

}
