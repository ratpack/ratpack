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

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import ratpack.func.Function;
import ratpack.registry.Registry;
import ratpack.util.Types;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public class MultiEntryRegistry implements Registry {

  private final List<? extends RegistryEntry<?>> entries;

  public MultiEntryRegistry(List<? extends RegistryEntry<?>> entries) {
    this.entries = entries;
  }

  @Override
  public String toString() {
    return "Registry{" + entries + '}';
  }

  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    ConcurrentMap<TypeToken<?>, Boolean> cache = TypeCaching.cache(type);
    for (RegistryEntry<?> entry : entries) {
      if (TypeCaching.isAssignableFrom(cache, type, entry.getType())) {
        @SuppressWarnings("unchecked") O cast = (O) entry.get();
        return Optional.of(cast);
      }
    }

    return Optional.empty();
  }

  public <O> Iterable<? extends O> getAll(final TypeToken<O> type) {
    ImmutableList.Builder<RegistryEntry<?>> builder = null;
    ConcurrentMap<TypeToken<?>, Boolean> cache = TypeCaching.cache(type);
    for (RegistryEntry<?> entry : entries) {
      if (TypeCaching.isAssignableFrom(cache, type, entry.getType())) {
        if (builder == null) {
          builder = ImmutableList.builder();
        }
        builder.add(entry);
      }
    }

    if (builder == null) {
      return Collections.emptyList();
    } else {
      return new EntryIterable<>(builder.build());
    }
  }

  @Override
  public <T, O> Optional<O> first(TypeToken<T> type, Function<? super T, ? extends O> function) throws Exception {
    ConcurrentMap<TypeToken<?>, Boolean> cache = TypeCaching.cache(type);
    for (RegistryEntry<?> entry : entries) {
      if (TypeCaching.isAssignableFrom(cache, type, entry.getType())) {
        RegistryEntry<? extends T> cast = Types.cast(entry);
        O result = function.apply(cast.get());
        if (result != null) {
          return Optional.of(result);
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MultiEntryRegistry that = (MultiEntryRegistry) o;

    return entries.equals(that.entries);
  }

  @Override
  public int hashCode() {
    return entries.hashCode();
  }

  private static class EntryIterable<T> implements Iterable<T> {
    private final ImmutableList<RegistryEntry<?>> entries;

    public EntryIterable(ImmutableList<RegistryEntry<?>> entries) {
      this.entries = entries;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      EntryIterable<?> that = (EntryIterable<?>) o;
      return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
      return entries.hashCode();
    }

    private static class Iter<T> implements Iterator<T> {
      private final Iterator<RegistryEntry<?>> entryIterator;

      public Iter(Iterator<RegistryEntry<?>> entryIterator) {
        this.entryIterator = entryIterator;
      }

      @Override
      public boolean hasNext() {
        return entryIterator.hasNext();
      }

      @SuppressWarnings("unchecked")
      @Override
      public T next() {
        return (T) entryIterator.next().get();
      }
    }

    @Override
    public Iterator<T> iterator() {
      return new Iter<>(entries.iterator());
    }
  }
}
