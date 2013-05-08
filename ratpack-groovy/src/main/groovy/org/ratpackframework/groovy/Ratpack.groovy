package org.ratpackframework.groovy

import org.ratpackframework.groovy.routing.Routing
import org.ratpackframework.guice.ModuleRegistry

interface Ratpack {

  void modules(@DelegatesTo(value = ModuleRegistry, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer)

  void routing(@DelegatesTo(value = Routing, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer)

}
