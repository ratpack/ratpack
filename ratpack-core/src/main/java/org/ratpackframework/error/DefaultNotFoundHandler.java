package org.ratpackframework.error;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

public class DefaultNotFoundHandler implements Handler<Routed<HttpExchange>> {

  @Override
  public void handle(Routed<HttpExchange> exchange) {
    exchange.get().end(HttpResponseStatus.NOT_FOUND);
  }

}
