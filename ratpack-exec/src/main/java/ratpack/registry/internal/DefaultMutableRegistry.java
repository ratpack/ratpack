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

import com.google.common.reflect.TypeToken;
import ratpack.api.Nullable;
import ratpack.func.Function;
import ratpack.registry.MutableRegistry;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

public class DefaultMutableRegistry implements MutableRegistry {

  private final Deque<RegistryEntry<?>> entries = new ConcurrentLinkedDeque<>();
  private final Registry registry = new MultiEntryRegistry(entries);

  @Override
  public <O> RegistrySpec addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    entries.push(new LazyRegistryEntry<>(type, supplier));
    return this;
  }

  @Override
  public <O> RegistrySpec add(TypeToken<O> type, O object) {
    entries.push(new DefaultRegistryEntry<>(type, object));
    return this;
  }

  @Override
  public <T> void remove(TypeToken<T> type) throws NotInRegistryException {
    Iterator<? extends RegistryEntry<?>> iterator = entries.iterator();
    while (iterator.hasNext()) {
      TypeToken<?> entryType = iterator.next().getType();
      if (TypeCaching.isAssignableFrom(TypeCaching.cache(entryType), entryType, type)) {
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

  @Override
  public <T, O> Optional<O> first(TypeToken<T> type, Function<? super T, ? extends O> function) throws Exception {
    return registry.first(type, function);
  }

}
