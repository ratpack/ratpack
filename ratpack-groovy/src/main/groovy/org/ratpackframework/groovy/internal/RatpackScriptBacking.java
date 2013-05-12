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

package org.ratpackframework.groovy.internal;

import groovy.lang.Closure;
import org.ratpackframework.util.Action;

public abstract class RatpackScriptBacking {

  private static final ThreadLocal<Action<Closure<?>>> BACKING_HOLDER = new ThreadLocal<Action<Closure<?>>>() {
    @Override
    protected Action<Closure<?>> initialValue() {
      return new StandaloneScriptBacking();
    }
  };

  public static Action<Closure<?>> getBacking() {
    return BACKING_HOLDER.get();
  }

  public static void withBacking(Action<Closure<?>> backing, Runnable runnable) {
    BACKING_HOLDER.set(backing);
    try {
      runnable.run();
    } finally {
      BACKING_HOLDER.remove();
    }
  }

}
