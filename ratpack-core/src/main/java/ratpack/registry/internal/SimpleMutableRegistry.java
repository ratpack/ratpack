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

package ratpack.registry.internal;

import com.google.common.base.Predicate;
import com.google.common.reflect.TypeToken;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.registry.MutableRegistry;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SimpleMutableRegistry implements MutableRegistry {

  private final List<RegistryEntry<?>> entries = new LinkedList<>();
  private final Registry registry = new MultiEntryRegistry(entries);

  @Override
  public <O> RegistrySpec addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    entries.add(new LazyRegistryEntry<>(type, supplier));
    return this;
  }

  @Override
  public <O> RegistrySpec add(TypeToken<? super O> type, O object) {
    entries.add(new DefaultRegistryEntry<>(type, object));
    return this;
  }

  @Override
  public <T> void remove(TypeToken<T> type) throws NotInRegistryException {
    Iterator<? extends RegistryEntry<?>> iterator = entries.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().getType().isAssignableFrom(type)) {
        iterator.remove();
      }
    }
  }

  @Override
  @Nullable
  public <T> Optional<T> maybeGet(TypeToken<T> type) {
    return registry.maybeGet(type);
  }

  @Override
  public <T> Iterable<? extends T> getAll(TypeToken<T> type) {
    return registry.getAll(type);
  }

  @Nullable
  @Override
  public <T> Optional<T> first(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.first(type, predicate);
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.all(type, predicate);
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    return registry.each(type, predicate, action);
  }
}
