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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

public class CachingRegistry implements Registry {

  private final Registry delegate;

  private final LoadingCache<Class<?>, ? extends Optional<?>> cache = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, Optional<?>>() {
    public Optional<?> load(Class<?> key) throws Exception {
      Object nullableReference = delegate.maybeGet(key);
      return Optional.fromNullable(nullableReference);
    }
  });

  private final LoadingCache<Class<?>, List<?>> allCache = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, List<?>>() {
    public List<?> load(Class<?> key) throws Exception {
      return delegate.getAll(key);
    }
  });

  public CachingRegistry(Registry delegate) {
    this.delegate = delegate;
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    O o = maybeGet(type);
    if (o == null) {
      throw new NotInRegistryException(type);
    } else {
      return o;
    }
  }

  @Override
  public <O> O maybeGet(Class<O> type) {
    try {
      @SuppressWarnings("unchecked") O o = (O) cache.get(type).orNull();
      return o;
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  @Override
  public <O> List<O> getAll(Class<O> type) {
    try {
      @SuppressWarnings("unchecked") List<O> objects = (List<O>) allCache.get(type);
      return objects;
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

}
