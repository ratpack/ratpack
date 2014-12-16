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

public class PathHandler implements Handler {

  private static final TypeToken<PathBinding> TYPE = TypeToken.of(PathBinding.class);

  private final PathBinder binding;
  private final Handler handler;

  public PathHandler(PathBinder binding, Handler handler) {
    this.binding = binding;
    this.handler = handler;
  }

  public void handle(Context context) {
    PathBinding childBinding = binding.bind(context.getRequest().getPath(), context.maybeGet(PathBinding.class).orElse(null));
    if (childBinding != null) {
      context.insert(new PathBindingRegistry(childBinding), handler);
    } else {
      context.next();
    }
  }

  private static class PathBindingRegistry implements Registry {

    private final PathBinding pathBinding;
    private final Optional<PathBinding> pathBindingOptional;

    public PathBindingRegistry(PathBinding pathBinding) {
      this.pathBinding = pathBinding;
      this.pathBindingOptional = Optional.of(pathBinding);
    }

    private <O> Set<? extends O> asSet() {
      return Types.cast(Collections.singleton(pathBinding));
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
      if (TYPE.isAssignableFrom(type) && predicate.apply(Types.<T>cast(pathBinding))) {
        return Types.cast(pathBindingOptional);
      } else {
        return Optional.empty();
      }
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
  }
}
