package org.ratpackframework.test

import com.google.inject.Module
import org.ratpackframework.app.Routing
import org.ratpackframework.assets.StaticAssetsConfig
import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.bootstrap.RatpackServerFactory
import org.ratpackframework.groovy.app.ClosureRouting
import org.ratpackframework.groovy.app.internal.RoutingScript
import org.ratpackframework.handler.Handler

class DefaultRatpackSpec extends RatpackSpec {

  StaticAssetsConfig staticAssets

  Closure<?> routingClosure = {}
  List<Module> modules = []

  def setup() {
    staticAssets = new StaticAssetsConfig(file("public"))
  }

  @Override
  File getAssetsDir() {
    staticAssets.directory
  }

  void routing(@DelegatesTo(org.ratpackframework.groovy.app.Routing) Closure routingClosure) {
    this.routingClosure = routingClosure
  }

  @Override
  RatpackServer createApp() {
    new RatpackServerFactory(0, null, null).create(new ClosureRouting(routingClosure), staticAssets, * modules)
  }
}
