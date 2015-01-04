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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import ratpack.func.Action;
import ratpack.registry.PredicateCacheability;
import ratpack.registry.Registry;
import ratpack.util.Types;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachingRegistry implements Registry {

  private final Registry delegate;

  private final ConcurrentMap<TypeToken<?>, Optional<?>> cache = new ConcurrentHashMap<>();
  private final ConcurrentMap<TypeToken<?>, Iterable<?>> allCache = new ConcurrentHashMap<>();
  private ConcurrentMap<PredicateCacheability.CacheKey<?>, Iterable<?>> predicateCache = new ConcurrentHashMap<>();

  public CachingRegistry(Registry delegate) {
    this.delegate = delegate;
  }

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return Types.cast(cache.computeIfAbsent(type, delegate::maybeGet));
  }

  @Override
  public <O> Iterable<O> getAll(TypeToken<O> type) {
    return Types.cast(allCache.computeIfAbsent(type, delegate::getAll));
  }

  private <T> Iterable<? extends T> getFromPredicateCache(TypeToken<T> type, Predicate<? super T> predicate) {
    return Types.cast(predicateCache.computeIfAbsent(new PredicateCacheability.CacheKey<>(type, predicate), k -> delegate.all(type, predicate)));
  }

  @Override
  public <T> Optional<T> first(TypeToken<T> type, Predicate<? super T> predicate) {
    if (PredicateCacheability.isCacheable(predicate)) {
      Iterable<? extends T> objects = getFromPredicateCache(type, predicate);
      return Optional.ofNullable(Iterables.getFirst(objects, null));
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

  @Override
  public String toString() {
    return "CachingRegistry{delegate=" + delegate + '}';
  }
}
