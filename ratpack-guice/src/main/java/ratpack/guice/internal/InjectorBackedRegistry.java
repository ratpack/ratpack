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

package ratpack.guice.internal;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Injector;
import com.google.inject.Provider;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.PredicateCacheability;
import ratpack.registry.Registry;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

public class InjectorBackedRegistry implements Registry {

  final Injector injector;

  public InjectorBackedRegistry(Injector injector) {
    this.injector = injector;
  }

  private final LoadingCache<TypeToken<?>, List<Provider<?>>> providerCache = CacheBuilder.newBuilder().build(new CacheLoader<TypeToken<?>, List<Provider<?>>>() {
    @Override
    public List<Provider<?>> load(@SuppressWarnings("NullableProblems") TypeToken<?> key) throws Exception {
      @SuppressWarnings({"unchecked", "RedundantCast"})
      List<Provider<?>> providers = (List<Provider<?>>) GuiceUtil.allProvidersOfType(injector, key);
      return providers;
    }
  });

  private final LoadingCache<PredicateCacheability.CacheKey<?>, List<Provider<?>>> predicateCache = CacheBuilder.newBuilder().build(new CacheLoader<PredicateCacheability.CacheKey<?>, List<Provider<?>>>() {
    @Override
    public List<Provider<?>> load(@SuppressWarnings("NullableProblems") PredicateCacheability.CacheKey<?> key) throws Exception {
      return get(key);
    }

    private <T> List<Provider<?>> get(PredicateCacheability.CacheKey<T> key) throws ExecutionException {
      List<Provider<?>> providers = getProviders(key.type);
      if (providers.isEmpty()) {
        return Collections.emptyList();
      } else {
        ImmutableList.Builder<Provider<?>> builder = ImmutableList.builder();
        Predicate<? super T> predicate = key.predicate;
        for (Provider<?> provider : providers) {
          @SuppressWarnings("unchecked") Provider<T> castProvider = (Provider<T>) provider;
          if (predicate.apply(castProvider.get())) {
            builder.add(castProvider);
          }
        }

        return builder.build();
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
    List<Provider<?>> providers = getProviders(type);
    if (providers.isEmpty()) {
      return null;
    } else {
      @SuppressWarnings("unchecked") T cast = (T) providers.get(0).get();
      return cast;
    }
  }

  private <T> List<Provider<?>> getProviders(TypeToken<T> type) {
    try {
      return providerCache.get(type);
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  @Override
  public <O> List<O> getAll(Class<O> type) {
    return getAll(TypeToken.of(type));
  }

  @Override
  public <O> List<O> getAll(TypeToken<O> type) {
    List<Provider<?>> providers = getProviders(type);
    if (providers.isEmpty()) {
      return Collections.emptyList();
    } else {
      return Lists.transform(providers, new Function<Provider<?>, O>() {
        @Override
        public O apply(Provider<?> input) {
          @SuppressWarnings("unchecked") O cast = (O) input.get();
          return cast;
        }
      });
    }
  }

  private <T> List<Provider<?>> getAll(TypeToken<T> type, Predicate<? super T> predicate) {
    try {
      return predicateCache.get(new PredicateCacheability.CacheKey<>(type, predicate));
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  @Nullable
  @Override
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    if (PredicateCacheability.isCacheable(predicate)) {
      List<Provider<?>> all = getAll(type, predicate);
      if (all.isEmpty()) {
        return null;
      } else {
        @SuppressWarnings("unchecked") T cast = (T) all.get(0).get();
        return cast;
      }
    } else {
      T object = maybeGet(type);
      if (object != null && predicate.apply(object)) {
        return object;
      } else {
        return null;
      }
    }
  }

  @Override
  public <T> List<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    if (PredicateCacheability.isCacheable(predicate)) {
      List<Provider<?>> all = getAll(type, predicate);
      @SuppressWarnings("unchecked") List<? extends T> cast = (List<? extends T>) all;
      return cast;
    } else {
      ImmutableList.Builder<T> builder = ImmutableList.builder();
      // no efficient Lists.filter() - http://stackoverflow.com/questions/8458663/guava-why-is-there-no-lists-filter-function
      for (T object : getAll(type)) {
        if (predicate.apply(object)) {
          builder.add(object);
        }
      }
      return builder.build();
    }
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    if (PredicateCacheability.isCacheable(predicate)) {
      List<? extends T> all = all(type, predicate);
      if (all.isEmpty()) {
        return false;
      } else {
        for (T t : all) {
          action.execute(t);
        }
        return true;
      }
    } else {
      boolean foundMatch = false;
      List<Provider<?>> providers = getProviders(type);
      for (Provider<?> provider : providers) {
        @SuppressWarnings("unchecked") T cast = (T) provider.get();
        if (predicate.apply(cast)) {
          action.execute(cast);
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

    InjectorBackedRegistry that = (InjectorBackedRegistry) o;

    return injector.equals(that.injector);
  }

  @Override
  public int hashCode() {
    return injector.hashCode();
  }

}
