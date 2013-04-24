package org.ratpackframework.routing;

import org.ratpackframework.Handler;

public class NoopRouter<T> implements Handler<Routed<T>> {

  @Override
  public void handle(Routed<T> event) {
    event.next();
  }

}
