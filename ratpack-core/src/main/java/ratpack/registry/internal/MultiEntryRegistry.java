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

import static ratpack.util.ExceptionUtils.uncheck;

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
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") T cast = (T) entry.get();
        if (predicate.apply(cast)) {
          return cast;
        }
      }
    }
    return null;
  }

  @Override
  public <T> List<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    ImmutableList.Builder<T> builder = ImmutableList.builder();
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") T cast = (T) entry.get();
        if (predicate.apply(cast)) {
          builder.add(cast);
        }
      }
    }
    return builder.build();
  }

  @Override
  public <T> boolean first(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) {
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") T cast = (T) entry.get();
        if (predicate.apply(cast)) {
          try {
            action.execute(cast);
          } catch(Exception e) {
            throw uncheck(e);
          }
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) {
    boolean foundMatch = false;
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        @SuppressWarnings("unchecked") T cast = (T) entry.get();
        if (predicate.apply(cast)) {
          try {
            action.execute(cast);
            foundMatch = true;
          } catch(Exception e) {
            throw uncheck(e);
          }
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
