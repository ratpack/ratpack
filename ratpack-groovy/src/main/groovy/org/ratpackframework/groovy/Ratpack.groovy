package org.ratpackframework.groovy

import org.ratpackframework.groovy.app.Routing
import org.ratpackframework.groovy.bootstrap.ModuleRegistry

interface Ratpack {

  void modules(@DelegatesTo(ModuleRegistry) Closure<?> configurer)

  void routing(@DelegatesTo(Routing) Closure<?> configurer)

}
