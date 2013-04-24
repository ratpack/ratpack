package org.ratpackframework.groovy.app;

import groovy.lang.Closure;
import org.ratpackframework.app.Routing;
import org.ratpackframework.groovy.app.internal.RoutingScript;
import org.ratpackframework.Handler;

public class ClosureRouting implements Handler<Routing> {

  private final Closure<?> closure;

  public ClosureRouting(Closure<?> closure) {
    this.closure = closure;
  }

  public void handle(Routing routing) {
    if (closure != null) {
      Closure<?> clone = (Closure<?>) closure.clone();
      clone.setDelegate(new RoutingScript(routing));
      clone.call();
    }
  }

}
