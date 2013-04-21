package org.ratpackframework.test

import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.bootstrap.RatpackServerBuilder
import org.ratpackframework.groovy.app.Routing
import org.ratpackframework.groovy.app.internal.ClosureAppFactory
import org.ratpackframework.groovy.bootstrap.ModuleRegistry
import org.ratpackframework.http.CoreHttpHandlers

class DefaultRatpackSpec extends RatpackSpec {

  Closure<?> routing = {}
  Closure<?> modules = {}

  void routing(@DelegatesTo(Routing) Closure<?> configurer) {
    this.routing = configurer
  }

  void modules(@DelegatesTo(ModuleRegistry) Closure<?> configurer) {
    this.modules = configurer
  }

  @Override
  RatpackServer createApp() {
    ClosureAppFactory appFactory = new ClosureAppFactory()
    CoreHttpHandlers coreHandlers = appFactory.create(modules, routing)

    RatpackServerBuilder builder = new RatpackServerBuilder(coreHandlers)
    builder.port = 0
    builder.host = null
    builder.build()
  }
}
