package org.ratpackframework.routing;

import org.ratpackframework.Action;

public interface Routed<T> {

  T get();

  void next();

  Routed<T> withNext(Action<Routed<T>> next);

  void error(Exception e);
}
