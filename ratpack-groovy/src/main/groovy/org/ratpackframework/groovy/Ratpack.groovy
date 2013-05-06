package org.ratpackframework.groovy

import org.ratpackframework.groovy.routing.Routing
import org.ratpackframework.guice.ModuleRegistry

interface Ratpack {

  void modules(@DelegatesTo(ModuleRegistry) Closure<?> configurer)

  void routing(@DelegatesTo(Routing) Closure<?> configurer)

}
