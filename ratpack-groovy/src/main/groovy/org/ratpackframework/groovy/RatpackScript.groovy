package org.ratpackframework.groovy

import org.ratpackframework.groovy.internal.RatpackScriptBacking

abstract class RatpackScript {

  static void ratpack(@DelegatesTo(value = Ratpack.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    RatpackScriptBacking.getBacking().execute(closure)
  }

}
