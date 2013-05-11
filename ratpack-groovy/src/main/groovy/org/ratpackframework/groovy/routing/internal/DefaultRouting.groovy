package org.ratpackframework.groovy.routing.internal

import groovy.transform.CompileStatic
import org.ratpackframework.groovy.ClosureHandlers
import org.ratpackframework.groovy.Closures
import org.ratpackframework.groovy.routing.Routing
import org.ratpackframework.routing.Exchange
import org.ratpackframework.routing.Handler
import org.ratpackframework.routing.Handlers

@CompileStatic
class DefaultRouting implements Routing {

  private final List<Handler> handlers
  private final Exchange exchange

  DefaultRouting(Exchange exchange, List<Handler> handlers) {
    this.exchange = exchange
    this.handlers = handlers
  }

  @Override
  void route(@DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    route(ClosureHandlers.handler(handler))
  }

  @Override
  void routes(@DelegatesTo(value = Routing, strategy = Closure.DELEGATE_FIRST) Closure<?> routes) {
    route(new RoutingHandler(Closures.action(routes)))
  }

  @Override
  void path(String path, @DelegatesTo(value = Routing, strategy = Closure.DELEGATE_FIRST) Closure<?> routing) {
    route(Handlers.path(path, new RoutingHandler(Closures.action(routing))))
  }

  @Override
  void all(String path, @DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    route(ClosureHandlers.handler(path, handler))
  }

  @Override
  void handler(String path, List<String> methods, @DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    route(ClosureHandlers.handler(path, methods, handler))
  }

  @Override
  void get(String path, @DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    route(ClosureHandlers.get(path, handler))
  }

  @Override
  void post(String path, @DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    route(ClosureHandlers.post(path, handler))
  }

  @Override
  void assets(String path, String... indexFiles) {
    route(Handlers.assets(path, indexFiles))
  }

  @Override
  Exchange getExchange() {
    exchange
  }

  @Override
  void route(Handler handler) {
    handlers.add(handler)
  }
}
