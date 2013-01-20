package com.bleedingwolf.ratpack.internal

import groovy.transform.CompileStatic
import org.vertx.java.core.Handler

@CompileStatic
class ClosureHandlerAdapter<T> implements Handler<T> {

  private final Closure<?> closure

  ClosureHandlerAdapter(Closure<?> closure) {
    this.closure = closure
  }

  @Override
  void handle(T event) {
    closure.call(event)
  }

}
