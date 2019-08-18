/*
 * Copyright 2015 the original author or authors.
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

package ratpack.guice.internal;

import com.google.inject.*;
import ratpack.exec.Execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ExecutionBasedScope<S extends Map<Key<?>, Object>> implements Scope {

  private final Class<S> storeType;
  private final List<Key<?>> keys = new ArrayList<>();
  private final String name;

  public ExecutionBasedScope(Class<S> storeType, String name) {
    this.storeType = storeType;
    this.name = name;
  }

  public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
    keys.add(key);
    return () -> {
      Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);

      @SuppressWarnings("unchecked")
      T current = (T) scopedObjects.get(key);
      if (current == null && !scopedObjects.containsKey(key)) {
        current = unscoped.get();

        // don't remember proxies; these exist only to serve circular dependencies
        if (Scopes.isCircularProxy(current)) {
          return current;
        }

        scopedObjects.put(key, current);
      }
      return current;
    };
  }

  abstract protected S createStore();

  private Map<Key<?>, Object> getScopedObjectMap(Key<?> key) {
    Execution execution = Execution.currentOpt().<OutOfScopeException>orElseThrow(() -> {
      throw outOfScopeException(key);
    });

    if (!inScope(execution)) {
      throw outOfScopeException(key);
    }

    return execution.maybeGet(storeType).orElseGet(() -> {
      S store = createStore();
      execution.add(storeType, store);
      return store;
    });
  }

  private OutOfScopeException outOfScopeException(Key<?> key) {
    return new OutOfScopeException("Cannot access " + key + " outside of " + name);
  }

  protected boolean inScope(Execution execution) {
    return true;
  }

  public List<Key<?>> getKeys() {
    return keys;
  }

}
