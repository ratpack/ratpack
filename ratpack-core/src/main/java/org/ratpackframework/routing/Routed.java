package org.ratpackframework.routing;

import org.ratpackframework.handler.Handler;

public interface Routed<T> {

  T get();

  void next();

  Routed<T> withNext(Handler<Routed<T>> next);

  void error(Exception e);
}
