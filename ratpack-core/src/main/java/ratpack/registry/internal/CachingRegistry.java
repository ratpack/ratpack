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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.registry.PredicateCacheability;
import ratpack.registry.Registry;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

public class CachingRegistry implements Registry {

  private final Registry delegate;

  private final LoadingCache<TypeToken<?>, ? extends Optional<?>> cache = CacheBuilder.newBuilder().build(new CacheLoader<TypeToken<?>, Optional<?>>() {
    public Optional<?> load(@SuppressWarnings("NullableProblems") TypeToken<?> key) throws Exception {
      Object nullableReference = delegate.maybeGet(key);
      return Optional.fromNullable(nullableReference);
    }
  });

  private final LoadingCache<TypeToken<?>, List<?>> allCache = CacheBuilder.newBuilder().build(new CacheLoader<TypeToken<?>, List<?>>() {
    public List<?> load(@SuppressWarnings("NullableProblems") TypeToken<?> key) throws Exception {
      return Lists.newArrayList(delegate.getAll(key));
    }
  });

  private LoadingCache<PredicateCacheability.CacheKey<?>, List<?>> predicateCache = CacheBuilder.newBuilder().build(new CacheLoader<PredicateCacheability.CacheKey<?>, List<?>>() {
    @Override
    public List<?> load(@SuppressWarnings("NullableProblems") PredicateCacheability.CacheKey<?> key) throws Exception {
      return Lists.newArrayList(getAll(key));
    }

    private <T> Iterable<? extends T> getAll(PredicateCacheability.CacheKey<T> key) {
      return delegate.all(key.type, key.predicate);
    }
  });

  public CachingRegistry(Registry delegate) {
    this.delegate = delegate;
  }

  @Override
  public <O> O maybeGet(TypeToken<O> type) {
    try {
      @SuppressWarnings("unchecked") O o = (O) cache.get(type).orNull();
      return o;
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  @Override
  public <O> List<O> getAll(TypeToken<O> type) {
    try {
      @SuppressWarnings("unchecked") List<O> objects = (List<O>) allCache.get(type);
      return objects;
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  private <T> List<T> getFromPredicateCache(TypeToken<T> type, Predicate<? super T> predicate) {
    try {
      @SuppressWarnings("unchecked") List<T> objects = (List<T>) predicateCache.get(new PredicateCacheability.CacheKey<>(type, predicate));
      return objects;
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  @Nullable
  @Override
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    if (PredicateCacheability.isCacheable(predicate)) {
      List<T> objects = getFromPredicateCache(type, predicate);
      if (objects.isEmpty()) {
        return null;
      } else {
        return objects.get(0);
      }
    } else {
      return delegate.first(type, predicate);
    }
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    if (PredicateCacheability.isCacheable(predicate)) {
      return getFromPredicateCache(type, predicate);
    } else {
      return delegate.all(type, predicate);
    }
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    Iterable<? extends T> all = all(type, predicate);
    boolean any = false;
    for (T item : all) {
      any = true;
      action.execute(item);
    }

    return any;
  }

}
