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

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import ratpack.func.Action;
import ratpack.registry.PredicateCacheability;
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

  public CachingBackedRegistry(RegistryBacking registryBacking) {
    this.registryBacking = registryBacking;
  }

  private final ConcurrentMap<TypeToken<?>, Iterable<? extends Supplier<?>>> supplierCache = new ConcurrentHashMap<>();
  private final ConcurrentMap<PredicateCacheability.CacheKey<?>, Iterable<? extends Supplier<?>>> predicateCache = new ConcurrentHashMap<>();

  public <T> Optional<T> maybeGet(TypeToken<T> type) {
    Iterator<? extends Supplier<T>> suppliers = getSuppliers(type).iterator();
    if (!suppliers.hasNext()) {
      return Optional.empty();
    } else {
      return Optional.of(suppliers.next().get());
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
    return Types.cast(compute(supplierCache, type, t -> registryBacking.provide(type)));
  }

  protected <T> Iterable<? extends Supplier<T>> getSuppliers(TypeToken<T> type, Predicate<? super T> predicate) {
    return Types.cast(
      compute(predicateCache, new PredicateCacheability.CacheKey<>(type, predicate), key ->
          Iterables.filter(getSuppliers(type), new Predicate<Supplier<T>>() {
            @Override
            public boolean apply(Supplier<T> input) {
              return predicate.apply(input.get());
            }
          })
      )
    );
  }

  @Override
  public <T> Optional<T> first(TypeToken<T> type, Predicate<? super T> predicate) {
    Iterator<? extends T> matching = all(type, predicate).iterator();
    if (!matching.hasNext()) {
      return Optional.empty();
    } else {
      return Optional.of(matching.next());
    }
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    if (PredicateCacheability.isCacheable(predicate)) {
      return transformToInstances(getSuppliers(type, predicate));
    } else {
      return Iterables.filter(getAll(type), predicate);
    }
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    if (PredicateCacheability.isCacheable(predicate)) {
      Iterable<? extends T> all = all(type, predicate);
      boolean any = false;
      for (T t : all) {
        any = true;
        action.execute(t);
      }
      return any;
    } else {
      boolean foundMatch = false;
      Iterable<? extends Supplier<T>> suppliers = getSuppliers(type);
      for (Supplier<T> supplier : suppliers) {
        T instance = supplier.get();
        if (predicate.apply(instance)) {
          action.execute(instance);
          foundMatch = true;
        }
      }
      return foundMatch;
    }
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
