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
import ratpack.func.Factory;
import ratpack.registry.MutableRegistry;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SimpleMutableRegistry<T> implements MutableRegistry<T> {

  private final List<RegistryEntry<? extends T>> entries = new LinkedList<>();
  private final Registry registry = new MultiEntryRegistry(entries);

  @Override
  public <O extends T> void register(Class<O> type, O object) {
    entries.add(new DefaultRegistryEntry<>(TypeToken.of(type), object));
  }

  @Override
  public void register(T object) {
    @SuppressWarnings("unchecked")
    Class<T> type = (Class<T>) object.getClass();
    TypeToken<T> typeToken = TypeToken.of(type);
    entries.add(new DefaultRegistryEntry<>(typeToken, object));
  }

  @Override
  public <O extends T> void registerLazy(Class<O> type, Factory<? extends O> factory) {
    entries.add(new LazyRegistryEntry<>(TypeToken.of(type), factory));
  }

  @Override
  public <O extends T> void remove(Class<O> type) throws NotInRegistryException {
    Iterator<? extends RegistryEntry<? extends T>> iterator = entries.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().getType().isAssignableFrom(type)) {
        iterator.remove();
      }
    }
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    return registry.get(type);
  }

  @Nullable
  @Override
  public <O> O maybeGet(Class<O> type) {
    return registry.maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(Class<O> type) {
    return registry.getAll(type);
  }

  @Override
  public <O> O get(TypeToken<O> type) throws NotInRegistryException {
    return registry.get(type);
  }

  @Override
  @Nullable
  public <O> O maybeGet(TypeToken<O> type) {
    return registry.maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return registry.getAll(type);
  }

  @Nullable
  @Override
  public <O> O first(TypeToken<O> type, Predicate<? super O> predicate) {
    return registry.first(type, predicate);
  }

  @Override
  public <O> Iterable<? extends O> all(TypeToken<O> type, Predicate<? super O> predicate) {
    return registry.all(type, predicate);
  }

  @Override
  public <O> boolean each(TypeToken<O> type, Predicate<? super O> predicate, Action<? super O> action) throws Exception {
    return registry.each(type, predicate, action);
  }
}
