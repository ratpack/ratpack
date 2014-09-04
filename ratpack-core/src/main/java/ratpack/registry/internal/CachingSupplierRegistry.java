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

import com.google.common.base.Function;
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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

/**
 *  Registry implementation which is backed by a Function that provides a list of Suppliers for a given TypeToken
 *
 *  This implementation caches lookups by type and by the combination of type and predicate (when the predicate is cacheable)
 */
public class CachingSupplierRegistry implements Registry {
  private final Function<TypeToken<?>, List<? extends Supplier<?>>> suppliersForTypeFunction;

  public CachingSupplierRegistry(Function<TypeToken<?>, List<? extends Supplier<?>>> suppliersForTypeFunction) {
    this.suppliersForTypeFunction = suppliersForTypeFunction;
  }

  private final LoadingCache<TypeToken<?>, List<? extends Supplier<?>>> supplierCache = CacheBuilder.newBuilder().build(new CacheLoader<TypeToken<?>, List<? extends Supplier<?>>>() {
    @Override
    public List<? extends Supplier<?>> load(@SuppressWarnings("NullableProblems") TypeToken<?> key) throws Exception {
      return Objects.firstNonNull(suppliersForTypeFunction.apply(key), ImmutableList.<Supplier<?>>of());
    }
  });

  private final LoadingCache<PredicateCacheability.CacheKey<?>, List<? extends Supplier<?>>> predicateCache = CacheBuilder.newBuilder().build(new CacheLoader<PredicateCacheability.CacheKey<?>, List<? extends Supplier<?>>>() {
    @Override
    public List<? extends Supplier<?>> load(@SuppressWarnings("NullableProblems") PredicateCacheability.CacheKey<?> key) throws Exception {
      return get(key);
    }

    private <T> List<? extends Supplier<?>> get(final PredicateCacheability.CacheKey<T> key) throws ExecutionException {
      List<? extends Supplier<?>> suppliers = getSuppliers(key.type);
      if (suppliers == null || suppliers.isEmpty()) {
        return ImmutableList.of();
      } else {
        return FluentIterable.from(suppliers).filter(new Predicate<Supplier<?>>() {
          @Override
          public boolean apply(Supplier<?> input) {
            @SuppressWarnings("unchecked") T instance = (T) input.get();
            return key.predicate.apply(instance);
          }
        }).toList();
      }
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
    List<? extends Supplier<?>> suppliers = getSuppliers(type);
    if (suppliers.isEmpty()) {
      return null;
    } else {
      @SuppressWarnings("unchecked") T cast = (T) suppliers.get(0).get();
      return cast;
    }
  }

  @Override
  public <O> Iterable<? extends O> getAll(Class<O> type) {
    return getAll(TypeToken.of(type));
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    final List<? extends Supplier<?>> suppliers = getSuppliers(type);
    if (suppliers.isEmpty()) {
      return Collections.emptyList();
    } else {
      return transformToInstances(suppliers);
    }
  }

  protected <O> Iterable<O> transformToInstances(Iterable<? extends Supplier<?>> suppliers) {
    return Iterables.transform(suppliers, new Function<Supplier<?>, O>() {
      @Override
      public O apply(Supplier<?> input) {
        @SuppressWarnings("unchecked") O cast = (O) input.get();
        return cast;
      }
    });
  }

  protected <T> List<? extends Supplier<?>> getSuppliers(TypeToken<T> type) {
    try {
      return supplierCache.get(type);
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  protected <T> List<? extends Supplier<?>> getSuppliers(TypeToken<T> type, Predicate<? super T> predicate) {
    try {
      return predicateCache.get(new PredicateCacheability.CacheKey<>(type, predicate));
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
      List<? extends Supplier<?>> suppliers = getSuppliers(type);
      for (Supplier<?> supplier : suppliers) {
        @SuppressWarnings("unchecked") T cast = (T) supplier.get();
        if (predicate.apply(cast)) {
          action.execute(cast);
          foundMatch = true;
        }
      }
      return foundMatch;
    }
  }

  /**
   * Invalidates the cached suppliers (supplierCache) and the cached filtered supplier results (predicateCache)
   */
  public void invalidateAll() {
    supplierCache.invalidateAll();
    predicateCache.invalidateAll();
  }
}
