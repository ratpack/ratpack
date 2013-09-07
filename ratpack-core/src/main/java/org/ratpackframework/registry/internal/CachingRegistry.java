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

package org.ratpackframework.registry.internal;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.ratpackframework.registry.NotInRegistryException;
import org.ratpackframework.registry.Registry;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class CachingRegistry<T> implements Registry<T> {

  private final Registry<T> delegate;

  private final LoadingCache<Class<? extends T>, ? extends Optional<? extends T>> cache = CacheBuilder.newBuilder().build(new CacheLoader<Class<? extends T>, Optional<? extends T>>() {
    public Optional<? extends T> load(Class<? extends T> key) throws Exception {
      T nullableReference = delegate.maybeGet(key);
      return Optional.fromNullable(nullableReference);
    }
  });

  private final LoadingCache<Class<? extends T>, List<? extends T>> allCache = CacheBuilder.newBuilder().build(new CacheLoader<Class<? extends T>, List<? extends T>>() {
    public List<? extends T> load(Class<? extends T> key) throws Exception {
      return delegate.getAll(key);
    }
  });

  public CachingRegistry(Registry<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public <O extends T> O get(Class<O> type) throws NotInRegistryException {
    O o = maybeGet(type);
    if (o == null) {
      throw new NotInRegistryException(delegate, type);
    } else {
      return o;
    }
  }

  @Override
  public <O extends T> O maybeGet(Class<O> type) {
    try {
      @SuppressWarnings("unchecked") O o = (O) cache.get(type).orNull();
      return o;
    } catch (ExecutionException e) {
      throw exception(e);
    }
  }

  private RuntimeException exception(ExecutionException e) {
    Throwable cause = e.getCause();
    if (cause instanceof RuntimeException) {
      return (RuntimeException) cause;
    } else {
      return new RuntimeException(cause);
    }
  }

  @Override
  public <O extends T> List<O> getAll(Class<O> type) {
    try {
      @SuppressWarnings("unchecked") List<O> objects = (List<O>) allCache.get(type);
      return objects;
    } catch (ExecutionException e) {
      throw exception(e);
    }
  }

  public static <T> Registry<T> cachingRegistry(Registry<T> registry) {
    return new CachingRegistry<>(registry);
  }
}
