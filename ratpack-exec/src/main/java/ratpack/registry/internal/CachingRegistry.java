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

package ratpack.registry.internal;

import com.google.common.reflect.TypeToken;
import ratpack.registry.Registry;
import ratpack.util.Types;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CachingRegistry implements Registry {

  private final Registry delegate;

  private final Map<TypeToken<?>, Optional<?>> cache = new ConcurrentHashMap<>();
  private final Map<TypeToken<?>, Iterable<?>> allCache = new ConcurrentHashMap<>();

  private final Function<TypeToken<?>, Optional<?>> maybeGet;
  private final Function<TypeToken<?>, Iterable<?>> getAll;

  public static Registry of(Registry registry) {
    if (registry instanceof CachingRegistry) {
      return registry;
    } else {
      return new CachingRegistry(registry);
    }
  }

  private CachingRegistry(Registry delegate) {
    this.delegate = delegate;
    this.maybeGet = delegate::maybeGet;
    this.getAll = delegate::getAll;
  }

  private static <K, V> V compute(Map<K, V> map, K key, Function<? super K, ? extends V> supplier) {
    V value = map.get(key);
    if (value == null) {
      value = supplier.apply(key);
      map.put(key, value);
    }
    return value;
  }

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return Types.cast(compute(cache, type, maybeGet));
  }

  @Override
  public <O> Iterable<O> getAll(TypeToken<O> type) {
    return Types.cast(compute(allCache, type, getAll));
  }

  @Override
  public String toString() {
    return "CachingRegistry{delegate=" + delegate + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CachingRegistry that = (CachingRegistry) o;

    return delegate.equals(that.delegate);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}
