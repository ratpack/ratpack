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

package ratpack.path.internal;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.path.PathBinder;
import ratpack.path.PathBinding;
import ratpack.registry.Registry;
import ratpack.util.Types;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class PathHandler implements Handler {

  private static final TypeToken<PathBinding> TYPE = TypeToken.of(PathBinding.class);

  private static final LoadingCache<CacheKey, Optional<Registry>> CACHE = CacheBuilder.newBuilder()
    .maximumSize(2048) // TODO - make this tuneable
    .build(new CacheLoader<CacheKey, Optional<Registry>>() {
      @Override
      public Optional<Registry> load(CacheKey key) throws Exception {
        Optional<PathBinding> binding = key.pathBinder.bind(key.path, key.parentBinding);
        if (binding.isPresent()) {
          return Optional.of(new PathBindingRegistry(binding));
        } else {
          return Optional.empty();
        }
      }
    });

  private static class CacheKey {
    private final PathBinder pathBinder;
    private final String path;
    private final Optional<PathBinding> parentBinding;

    public CacheKey(PathBinder pathBinder, String path, Optional<PathBinding> parentBinding) {
      this.pathBinder = pathBinder;
      this.path = path;
      this.parentBinding = parentBinding;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      CacheKey cacheKey = (CacheKey) o;

      return parentBinding.equals(cacheKey.parentBinding) && path.equals(cacheKey.path) && pathBinder.equals(cacheKey.pathBinder);
    }

    @Override
    public int hashCode() {
      int result = pathBinder.hashCode();
      result = 31 * result + path.hashCode();
      result = 31 * result + parentBinding.hashCode();
      return result;
    }
  }

  private final PathBinder binder;
  private final Handler handler;

  public PathHandler(PathBinder binder, Handler handler) {
    this.binder = binder;
    this.handler = handler;
  }

  public void handle(Context context) throws ExecutionException {
    Optional<Registry> registry = CACHE.get(new CacheKey(binder, context.getRequest().getPath(), context.maybeGet(PathBinding.class)));
    if (registry.isPresent()) {
      context.insert(registry.get(), handler);
    } else {
      context.next();
    }
  }

  private static class PathBindingRegistry implements Registry {

    private final Optional<PathBinding> pathBindingOptional;

    public PathBindingRegistry(Optional<PathBinding> pathBindingOptional) {
      this.pathBindingOptional = pathBindingOptional;
    }

    private <O> Set<? extends O> asSet() {
      return Types.cast(Collections.singleton(pathBindingOptional.orElse(null)));
    }

    @Override
    public <O> Optional<O> maybeGet(Class<O> type) {
      return PathBinding.class.isAssignableFrom(type) ? Types.cast(pathBindingOptional) : Optional.empty();
    }

    @Override
    public <O> Optional<O> maybeGet(TypeToken<O> type) {
      return TYPE.isAssignableFrom(type) ? Types.cast(pathBindingOptional) : Optional.empty();
    }

    @Override
    public <O> Set<? extends O> getAll(TypeToken<O> type) {
      return TYPE.isAssignableFrom(type) ? asSet() : Collections.emptySet();
    }

    @Override
    public <O> Set<? extends O> getAll(Class<O> type) {
      return PathBinding.class.isAssignableFrom(type) ? asSet() : Collections.emptySet();
    }

    @Override
    public <T> Optional<T> first(TypeToken<T> type, Predicate<? super T> predicate) {
      return Types.cast(pathBindingOptional.filter(p -> TYPE.isAssignableFrom(type) && predicate.apply(Types.<T>cast(p))));
    }

    @Override
    public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
      return first(type, predicate).map(Collections::singleton).orElseGet(Collections::emptySet);
    }

    @Override
    public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
      Optional<T> first = first(type, predicate);
      if (first.isPresent()) {
        action.execute(first.get());
        return true;
      } else {
        return false;
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

      PathBindingRegistry that = (PathBindingRegistry) o;

      return pathBindingOptional.equals(that.pathBindingOptional);
    }

    @Override
    public int hashCode() {
      return pathBindingOptional.hashCode();
    }
  }
}
