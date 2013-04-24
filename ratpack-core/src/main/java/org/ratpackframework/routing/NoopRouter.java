package org.ratpackframework.routing;

import org.ratpackframework.Action;

public class NoopRouter<T> implements Action<Routed<T>> {

  @Override
  public void execute(Routed<T> event) {
    event.next();
  }

}
