package org.ratpackframework.routing;

import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;

public class NoopRouter<T> implements Handler<Routed<T>> {

  @Override
  public void handle(Routed<T> event) {
    event.next();
  }

}
