/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.groovy.internal;

import groovy.lang.Closure;
import ratpack.func.Action;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class RatpackScriptBacking {

  private static final ThreadLocal<Lock> LOCK_HOLDER = new ThreadLocal<Lock>() {
    @Override
    protected Lock initialValue() {
      return new ReentrantLock();
    }
  };

  private static final ThreadLocal<Action<Closure<?>>> BACKING_HOLDER = new InheritableThreadLocal<Action<Closure<?>>>() {
    @Override
    protected Action<Closure<?>> initialValue() {
      return new StandaloneScriptBacking();
    }
  };

  public static void withBacking(Action<Closure<?>> backing, Runnable runnable) {
    LOCK_HOLDER.get().lock();
    try {
      Action<Closure<?>> previousBacking = BACKING_HOLDER.get();
      BACKING_HOLDER.set(backing);
      try {
        runnable.run();
      } finally {
        BACKING_HOLDER.set(previousBacking);
      }
    } finally {
      LOCK_HOLDER.get().unlock();
    }
  }

  public static Action<Closure<?>> swapBacking(Action<Closure<?>> backing) {
    LOCK_HOLDER.get().lock();
    try {
      Action<Closure<?>> previousBacking = BACKING_HOLDER.get();
      BACKING_HOLDER.set(backing);
      return previousBacking;
    } finally {
      LOCK_HOLDER.get().unlock();
    }
  }

  public static void execute(Closure<?> closure) throws Exception {
    LOCK_HOLDER.get().lock();
    try {
      BACKING_HOLDER.get().execute(closure);
    } finally {
      LOCK_HOLDER.get().unlock();
    }
  }

}
