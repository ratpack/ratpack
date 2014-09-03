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

package ratpack.spring.internal;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.PredicateCacheability;
import ratpack.registry.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

public class SpringBackedRegistry implements Registry {

  private final ListableBeanFactory beanFactory;

  public SpringBackedRegistry(ListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  private final LoadingCache<TypeToken<?>, List<ObjectFactory<?>>> objectFactoryCache = CacheBuilder.newBuilder().build(new CacheLoader<TypeToken<?>, List<ObjectFactory<?>>>() {
    @Override
    public List<ObjectFactory<?>> load(@SuppressWarnings("NullableProblems") TypeToken<?> key) throws Exception {
      @SuppressWarnings({"unchecked", "RedundantCast"})
      ImmutableList.Builder<ObjectFactory<?>> builder = ImmutableList.builder();
      for (final String beanName : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
        key.getRawType())) {
        builder.add(new ObjectFactory<Object>() {
          @Override
          public Object getObject() throws BeansException {
            return beanFactory.getBean(beanName);
          }
        });
      }
      return builder.build();
    }
  });

  private final LoadingCache<PredicateCacheability.CacheKey<?>, List<ObjectFactory<?>>> predicateCache = CacheBuilder.newBuilder().build(new CacheLoader<PredicateCacheability.CacheKey<?>, List<ObjectFactory<?>>>() {
    @Override
    public List<ObjectFactory<?>> load(@SuppressWarnings("NullableProblems") PredicateCacheability.CacheKey<?> key) throws Exception {
      return get(key);
    }

    private <T> List<ObjectFactory<?>> get(PredicateCacheability.CacheKey<T> key) throws ExecutionException {
      List<ObjectFactory<?>> objectFactories = getObjectFactories(key.type);
      if (objectFactories.isEmpty()) {
        return Collections.emptyList();
      } else {
        ImmutableList.Builder<ObjectFactory<?>> builder = ImmutableList.builder();
        Predicate<? super T> predicate = key.predicate;
        for (ObjectFactory<?> objectFactory : objectFactories) {
          @SuppressWarnings("unchecked") ObjectFactory<T> castObjectFactory = (ObjectFactory<T>) objectFactory;
          if (predicate.apply(castObjectFactory.getObject())) {
            builder.add(castObjectFactory);
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
    List<ObjectFactory<?>> objectFactories = getObjectFactories(type);
    if (objectFactories.isEmpty()) {
      return null;
    } else {
      @SuppressWarnings("unchecked") T cast = (T) objectFactories.get(0).getObject();
      return cast;
    }
  }

  private <T> List<ObjectFactory<?>> getObjectFactories(TypeToken<T> type) {
    try {
      return objectFactoryCache.get(type);
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  @Override
  public <O> Iterable<? extends O> getAll(Class<O> type) {
    return getAll(TypeToken.of(type));
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    final List<ObjectFactory<?>> objectFactories = getObjectFactories(type);
    if (objectFactories.isEmpty()) {
      return Collections.emptyList();
    } else {
      return transformToInstances(objectFactories);
    }
  }

  private <O> Iterable<O> transformToInstances(Iterable<ObjectFactory<?>> objectFactories) {
    return Iterables.transform(objectFactories, new Function<ObjectFactory<?>, O>() {
      @Override
      public O apply(ObjectFactory<?> input) {
        @SuppressWarnings("unchecked") O cast = (O) input.getObject();
        return cast;
      }
    });
  }

  private <T> List<ObjectFactory<?>> getAll(TypeToken<T> type, Predicate<? super T> predicate) {
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
      return transformToInstances(getAll(type, predicate));
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
      List<ObjectFactory<?>> objectFactories = getObjectFactories(type);
      for (ObjectFactory<?> objectFactory : objectFactories) {
        @SuppressWarnings("unchecked") T cast = (T) objectFactory.getObject();
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

    SpringBackedRegistry that = (SpringBackedRegistry) o;

    return beanFactory.equals(that.beanFactory);
  }

  @Override
  public int hashCode() {
    return beanFactory.hashCode();
  }

}
