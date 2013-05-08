package org.ratpackframework.groovy;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.ratpackframework.groovy.internal.RatpackScriptBacking;

public abstract class RatpackScript {

  public static void ratpack(@DelegatesTo(value = Ratpack.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    RatpackScriptBacking.getBacking().execute(closure);
  }

}
