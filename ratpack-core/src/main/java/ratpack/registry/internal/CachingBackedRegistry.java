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

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.PredicateCacheability;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBacking;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

public class CachingBackedRegistry implements Registry {
  private final RegistryBacking registryBacking;

  public CachingBackedRegistry(RegistryBacking registryBacking) {
    this.registryBacking = registryBacking;
  }

  private final LoadingCache<TypeToken<?>, Iterable<? extends Supplier<?>>> supplierCache = CacheBuilder.newBuilder().build(new CacheLoader<TypeToken<?>, Iterable<? extends Supplier<?>>>() {
    @Override
    public Iterable<? extends Supplier<?>> load(@SuppressWarnings("NullableProblems") TypeToken<?> key) throws Exception {
      return Objects.firstNonNull(registryBacking.provide(key), ImmutableList.<Supplier<?>>of());
    }
  });

  private final LoadingCache<PredicateCacheability.CacheKey<?>, Iterable<? extends Supplier<?>>> predicateCache = CacheBuilder.newBuilder().build(new CacheLoader<PredicateCacheability.CacheKey<?>, Iterable<? extends Supplier<?>>>() {
    @Override
    public Iterable<? extends Supplier<?>> load(@SuppressWarnings("NullableProblems") PredicateCacheability.CacheKey<?> key) throws Exception {
      return get(key);
    }

    private <T> Iterable<? extends Supplier<T>> get(final PredicateCacheability.CacheKey<T> key) throws ExecutionException {
      Iterable<? extends Supplier<T>> suppliers = getSuppliers(key.type);
      return FluentIterable.from(suppliers).filter(new Predicate<Supplier<T>>() {
        @Override
        public boolean apply(Supplier<T> input) {
          return key.predicate.apply(input.get());
        }
      }).toList();
    }
  });

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    return get(TypeToken.of(type));
  }

  @Override
  public <O> O get(TypeToken<O> type) throws NotInRegistryException {
    O object = maybeGet(type);
    if (object == null) {
      throw new NotInRegistryException(type);
    } else {
      return object;
    }
  }

  public <T> T maybeGet(Class<T> type) {
    return maybeGet(TypeToken.of(type));
  }

  public <T> T maybeGet(TypeToken<T> type) {
    Iterator<? extends Supplier<T>> suppliers = getSuppliers(type).iterator();
    if (!suppliers.hasNext()) {
      return null;
    } else {
      return suppliers.next().get();
    }
  }

  @Override
  public <O> Iterable<? extends O> getAll(Class<O> type) {
    return getAll(TypeToken.of(type));
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return transformToInstances(getSuppliers(type));
  }

  protected <O> Iterable<O> transformToInstances(Iterable<? extends Supplier<O>> suppliers) {
    return Iterables.transform(suppliers, Supplier::get);
  }

  protected <T> Iterable<? extends Supplier<T>> getSuppliers(TypeToken<T> type) {
    try {
      @SuppressWarnings("unchecked") Iterable<? extends Supplier<T>> suppliers = (Iterable<? extends Supplier<T>>) supplierCache.get(type);
      return Objects.firstNonNull(suppliers, ImmutableList.<Supplier<T>>of());
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  protected <T> Iterable<? extends Supplier<T>> getSuppliers(TypeToken<T> type, Predicate<? super T> predicate) {
    try {
      @SuppressWarnings("unchecked") Iterable<? extends Supplier<T>> suppliers = (Iterable<? extends Supplier<T>>) predicateCache.get(new PredicateCacheability.CacheKey<>(type, predicate));
      return Objects.firstNonNull(suppliers, ImmutableList.<Supplier<T>>of());
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  @Nullable
  @Override
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    Iterator<? extends T> matching = all(type, predicate).iterator();
    if (!matching.hasNext()) {
      return null;
    } else {
      return matching.next();
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
