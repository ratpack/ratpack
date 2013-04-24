package org.ratpackframework.error;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

/**
 * Simply sends a HTTP 404 back to the client.
 */
public class DefaultNotFoundHandler implements Handler<Routed<HttpExchange>> {

  @Override
  public void handle(Routed<HttpExchange> exchange) {
    exchange.get().end(HttpResponseStatus.NOT_FOUND);
  }

}
