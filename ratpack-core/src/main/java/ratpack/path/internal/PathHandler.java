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

import com.google.common.reflect.TypeToken;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.path.PathBinder;
import ratpack.path.PathBinding;
import ratpack.registry.Registry;
import ratpack.registry.internal.EmptyRegistry;
import ratpack.util.internal.BoundedConcurrentHashMap;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class PathHandler implements Handler {

  private static final TypeToken<PathBinding> PATH_BINDING_TYPE_TOKEN = TypeToken.of(PathBinding.class);

  private static final int CACHE_SIZE = Integer.parseInt(System.getProperty("ratpack.cachesize.path", "10000"));

  private final PathBinder binder;
  private final Handler[] handler;
  private final ConcurrentMap<PathBinding, Registry> cache = new BoundedConcurrentHashMap<>(CACHE_SIZE, Runtime.getRuntime().availableProcessors());

  public PathHandler(PathBinder binder, Handler handler) {
    this.binder = binder;
    this.handler = new Handler[]{handler};
  }

  public void handle(Context context) throws ExecutionException {
    PathBinding pathBinding = context.get(PATH_BINDING_TYPE_TOKEN);
    Registry registry = cache.get(pathBinding);
    if (registry == null) {
      registry = cache.computeIfAbsent(pathBinding, binding -> binder.bind(binding).map(Registry::single).orElse(Registry.empty()));
    }

    if (registry == EmptyRegistry.INSTANCE) {
      context.next();
    } else {
      context.insert(registry, handler);
    }
  }

}
