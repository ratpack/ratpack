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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.registry.Registry;

import java.util.Iterator;
import java.util.List;

public class MultiEntryRegistry implements Registry {

  private final List<? extends RegistryEntry<?>> entries;

  public MultiEntryRegistry(List<? extends RegistryEntry<?>> entries) {
    this.entries = entries;
  }

  @Override
  public String toString() {
    return "Registry{" + entries + '}';
  }

  public <O> O maybeGet(TypeToken<O> type) {
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") O cast = (O) entry.get();
        return cast;
      }
    }

    return null;
  }

  public <O> Iterable<? extends O> getAll(final TypeToken<O> type) {
    return () -> new Iterator<O>() {

      final Iterator<? extends RegistryEntry<?>> delegate = entries.iterator();
      O next;

      @Override
      public boolean hasNext() {
        if (next != null) {
          return true;
        }

        while (delegate.hasNext()) {
          RegistryEntry<?> entry = delegate.next();
          if (type.isAssignableFrom(entry.getType())) {
            @SuppressWarnings("unchecked") O cast = (O) entry.get();
            next = cast;
            return true;
          }
        }

        return false;
      }

      @Override
      public O next() {
        O nextCopy = next;
        next = null;
        return nextCopy;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Nullable
  @Override
  public <O> O first(TypeToken<O> type, Predicate<? super O> predicate) {
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") O cast = (O) entry.get();
        if (predicate.apply(cast)) {
          return cast;
        }
      }
    }
    return null;
  }

  @Override
  public <O> Iterable<? extends O> all(TypeToken<O> type, Predicate<? super O> predicate) {

    ImmutableList.Builder<O> builder = ImmutableList.builder();
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") O cast = (O) entry.get();
        if (predicate.apply(cast)) {
          builder.add(cast);
        }
      }
    }
    return builder.build();
  }

  @Override
  public <O> boolean each(TypeToken<O> type, Predicate<? super O> predicate, Action<? super O> action) throws Exception {
    boolean foundMatch = false;
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") O cast = (O) entry.get();
        if (predicate.apply(cast)) {
          action.execute(cast);
          foundMatch = true;
        }
      }
    }
    return foundMatch;
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
}
