package org.ratpackframework.test.groovy

import org.ratpackframework.groovy.handling.Chain
import org.ratpackframework.groovy.handling.internal.ChainBuildingHandler
import org.ratpackframework.handling.Handler
import org.ratpackframework.test.DefaultRatpackSpec

import static org.ratpackframework.groovy.Closures.action

abstract class RatpackGroovyDslSpec extends DefaultRatpackSpec {

  void handlers(@DelegatesTo(Chain) Closure<?> configurer) {
    this.handlersClosure = configurer
  }

  @Override
  protected Handler createHandler() {
    return new ChainBuildingHandler(action(handlersClosure))
  }
}
