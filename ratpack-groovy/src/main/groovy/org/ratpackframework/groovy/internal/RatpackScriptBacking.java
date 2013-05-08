package org.ratpackframework.groovy.internal;

import groovy.lang.Closure;
import org.ratpackframework.Action;

public abstract class RatpackScriptBacking {

  private static final ThreadLocal<Action<Closure<?>>> backingHolder = new ThreadLocal<Action<Closure<?>>>() {
    @Override
    protected Action<Closure<?>> initialValue() {
      return new StandaloneScriptBacking();
    }
  };

  public static Action<Closure<?>> getBacking() {
    return backingHolder.get();
  }

  public static void withBacking(Action<Closure<?>> backing, Runnable runnable) {
    backingHolder.set(backing);
    try {
      runnable.run();
    } finally {
      backingHolder.remove();
    }
  }

}
