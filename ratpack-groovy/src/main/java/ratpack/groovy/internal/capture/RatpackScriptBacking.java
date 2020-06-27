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

package ratpack.groovy.internal.capture;

import groovy.lang.Closure;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.groovy.internal.StandaloneScriptBacking;

public abstract class RatpackScriptBacking {

  private static final ThreadLocal<Action<Closure<?>>> BACKING_HOLDER = new InheritableThreadLocal<Action<Closure<?>>>() {
    @Override
    protected Action<Closure<?>> initialValue() {
      return StandaloneScriptBacking.INSTANCE;
    }
  };

  public static void withBacking(Action<Closure<?>> backing, Block runnable) throws Exception {
    Action<Closure<?>> previousBacking = BACKING_HOLDER.get();
    BACKING_HOLDER.set(backing);
    try {
      runnable.execute();
    } finally {
      BACKING_HOLDER.set(previousBacking);
    }
  }

  public static void execute(Closure<?> closure) throws Exception {
    BACKING_HOLDER.get().execute(closure);
  }

}
