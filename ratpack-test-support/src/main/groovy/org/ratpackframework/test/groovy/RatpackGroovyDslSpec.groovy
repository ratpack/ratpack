package org.ratpackframework.test.groovy

import org.ratpackframework.groovy.handling.Routing
import org.ratpackframework.groovy.handling.internal.RoutingHandler
import org.ratpackframework.handling.Handler
import org.ratpackframework.test.DefaultRatpackSpec

import static org.ratpackframework.groovy.Closures.action

class RatpackGroovyDslSpec extends DefaultRatpackSpec {

  void routing(@DelegatesTo(Routing) Closure<?> configurer) {
    this.routingCallback = configurer
  }

  @Override
  protected Handler createHandler() {
    return new RoutingHandler(action(routingCallback))
  }
}
