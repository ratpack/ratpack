/*
 * Copyright 2014 the original author or authors.
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

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBacking;
import ratpack.util.Types;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class CachingBackedRegistry implements Registry {

  private final RegistryBacking registryBacking;
  private final ConcurrentMap<TypeToken<?>, Iterable<? extends Supplier<?>>> supplierCache = new ConcurrentHashMap<>();

  public CachingBackedRegistry(RegistryBacking registryBacking) {
    this.registryBacking = registryBacking;
  }

  public <T> Optional<T> maybeGet(TypeToken<T> type) {
    Iterator<? extends Supplier<T>> suppliers = getSuppliers(type).iterator();
    if (!suppliers.hasNext()) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(suppliers.next().get());
    }
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
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return transformToInstances(getSuppliers(type));
  }

  protected <O> Iterable<O> transformToInstances(Iterable<? extends Supplier<O>> suppliers) {
    //noinspection Convert2MethodRef
    return Iterables.transform(suppliers, (s) -> s.get());
  }

  protected <T> Iterable<? extends Supplier<T>> getSuppliers(TypeToken<T> type) {
    Iterable<? extends Supplier<?>> suppliers = compute(supplierCache, type, t ->
        registryBacking.provide(type)
    );
    return Types.cast(suppliers);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CachingBackedRegistry that = (CachingBackedRegistry) o;

    return registryBacking.equals(that.registryBacking);
  }

  @Override
  public int hashCode() {
    return registryBacking.hashCode();
  }
}
