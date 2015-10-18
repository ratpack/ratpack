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
import ratpack.handling.internal.ChainHandler;
import ratpack.path.PathBinder;
import ratpack.path.PathBinding;
import ratpack.util.internal.BoundedConcurrentHashMap;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class PathHandler implements Handler {

  private static final TypeToken<PathBinding> PATH_BINDING_TYPE_TOKEN = TypeToken.of(PathBinding.class);

  private static final int CACHE_SIZE = Integer.parseInt(System.getProperty("ratpack.cachesize.path", "10000"));
  private static final Handler POP_BINDING = ctx -> {
    PathBindingStorage.pop();
    ctx.next();
  };

  private final PathBinder binder;
  private final Handler[] handler;
  private final ConcurrentMap<PathBinding, Optional<PathBinding>> cache = new BoundedConcurrentHashMap<>(CACHE_SIZE, Runtime.getRuntime().availableProcessors());

  public PathHandler(PathBinder binder, Handler handler) {
    this.binder = binder;
    Handler[] unpacked = ChainHandler.unpack(handler);
    Handler[] withPop = new Handler[unpacked.length + 1];
    System.arraycopy(unpacked, 0, withPop, 0, unpacked.length);
    withPop[unpacked.length] = POP_BINDING;
    this.handler = withPop;
  }

  public void handle(Context ctx) throws ExecutionException {
    PathBinding pathBinding = PathBindingStorage.get();
    Optional<PathBinding> newBinding = cache.get(pathBinding);
    if (newBinding == null) {
      newBinding = cache.computeIfAbsent(pathBinding, binder::bind);
    }

    if (newBinding.isPresent()) {
      PathBindingStorage.push(newBinding.get());
      ctx.insert(handler);
    } else {
      ctx.next();
    }
  }

}
