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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.path.PathBinder;
import ratpack.path.PathBinding;
import ratpack.registry.Registry;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class PathHandler implements Handler {

  private static final LoadingCache<CacheKey, Optional<Registry>> CACHE = CacheBuilder.newBuilder()
    .maximumSize(2048) // TODO - make this tuneable
    .build(new CacheLoader<CacheKey, Optional<Registry>>() {
      @Override
      public Optional<Registry> load(CacheKey key) throws Exception {
        return key.pathBinder.bind(key.path, key.parentBinding).map(b ->
            Registry.single(PathBinding.class, b)
        );
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

}
