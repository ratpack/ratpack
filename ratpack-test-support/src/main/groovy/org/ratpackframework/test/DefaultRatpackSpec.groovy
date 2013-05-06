package org.ratpackframework.test

import org.ratpackframework.http.Handler
import org.ratpackframework.http.Handlers
import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.bootstrap.RatpackServerBuilder
import org.ratpackframework.guice.DefaultGuiceBackedHandlerFactory
import org.ratpackframework.guice.ModuleRegistry
import org.ratpackframework.routing.Routing

import static org.ratpackframework.groovy.Closures.action

class DefaultRatpackSpec extends RatpackSpec {

  Closure<?> routing = {}
  Closure<?> modules = {}

  void routing(@DelegatesTo(Routing) Closure<?> configurer) {
    this.routing = configurer
  }

  void modules(@DelegatesTo(ModuleRegistry) Closure<?> configurer) {
    this.modules = configurer
  }

  void app(Closure<?> configurer) {
    configurer.call()
    startApp()
  }

  @Override
  RatpackServer createApp() {
    DefaultGuiceBackedHandlerFactory appFactory = new DefaultGuiceBackedHandlerFactory()
    def routingAction = action(routing)
    def modulesAction = action(modules)

    def withFsContext = Handlers.fsContext(dirPath, routingAction)

    Handler handler = appFactory.create(modulesAction, withFsContext)

    RatpackServerBuilder builder = new RatpackServerBuilder(handler)
    builder.port = 0
    builder.host = null
    builder.build()
  }
}
