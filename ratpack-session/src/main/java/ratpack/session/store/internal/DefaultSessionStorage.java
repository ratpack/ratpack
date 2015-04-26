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

package ratpack.session.store.internal;

import ratpack.exec.ExecControl;
import ratpack.exec.Fulfiller;
import ratpack.exec.Promise;
import ratpack.exec.Result;
import ratpack.session.store.SessionStorage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultSessionStorage implements SessionStorage {

  private final ConcurrentMap<String, Object> store;
  private ExecControl execControl;

  public DefaultSessionStorage(ConcurrentMap<String, Object> store, ExecControl execControl) {
    this.store = store;
    this.execControl = execControl;
  }


  @Override
  public <T> Promise<Optional<T>> get(String key, Class<T> type) {
    return execControl.blocking(() -> {
      Object value = store.get(key);
      if (value == null) {
        return Optional.empty();
      } else {
        return Optional.of(type.cast(value));
      }
    });
  }

  @Override
  public Promise<Boolean> set(String key, Object value) {
    return execControl.blocking(() -> {
      store.put(key, value);
      return true;
    });
  }


  @Override
  public Promise<Integer> remove(String key) {
    return execControl.blocking(() -> {
      Object lastValue = store.remove(key);
      if (lastValue == null) {
        return 0;
      } else {
        return 1;
      }
    });
  }

  @Override
  public Promise<Integer> clear() {
    return execControl.blocking(() -> {
      store.clear();
      return 1;
    });
  }

  @Override
  public Promise<Set<String>> getKeys() {
    return execControl.blocking(store::keySet);
  }


  @Override
  public boolean equals(Object o) {
    return store.equals(o);
  }

  @Override
  public int hashCode() {
    return store.hashCode();
  }

}
