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
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;

import java.util.List;

public class MultiEntryRegistry<T> implements Registry {

  private final List<RegistryEntry<? extends T>> entries;

  public MultiEntryRegistry(List<RegistryEntry<? extends T>> entries) {
    this.entries = entries;
  }

  @Override
  public String toString() {
    return "Registry{" + entries + '}';
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    return get(TypeToken.of(type));
  }

  @Override
  public <O> O get(TypeToken<O> type) throws NotInRegistryException {
    O object = maybeGet(type);
    if (object == null) {
      throw new NotInRegistryException(type);
    } else {
      return object;
    }
  }

  public <O> O maybeGet(Class<O> type) {
    return maybeGet(TypeToken.of(type));
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

  public <O> List<O> getAll(Class<O> type) {
    return getAll(TypeToken.of(type));
  }

  public <O> List<O> getAll(TypeToken<O> type) {
    ImmutableList.Builder<O> builder = ImmutableList.builder();
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") O cast = (O) entry.get();
        builder.add(cast);
      }
    }
    return builder.build();
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
  public <O> List<? extends O> all(TypeToken<O> type, Predicate<? super O> predicate) {
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
  public <O> boolean first(TypeToken<O> type, Predicate<? super O> predicate, Action<? super O> action) throws Exception {
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") O cast = (O) entry.get();
        if (predicate.apply(cast)) {
          action.execute(cast);
          return true;
        }
      }
    }
    return false;
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

    MultiEntryRegistry<?> that = (MultiEntryRegistry) o;

    return entries.equals(that.entries);
  }

  @Override
  public int hashCode() {
    return entries.hashCode();
  }
}
