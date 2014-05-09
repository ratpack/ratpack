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
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;

import java.util.List;

public class SingleEntryRegistry implements Registry {

  private final RegistryEntry<?> entry;

  public SingleEntryRegistry(RegistryEntry<?> entry) {
    this.entry = entry;
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    return get(TypeToken.of(type));
  }

  @Override
  public <O> O get(TypeToken<O> type) throws NotInRegistryException {
    O value = maybeGet(type);
    if (value == null) {
      throw new NotInRegistryException(type);
    } else {
      return value;
    }
  }


  @Nullable
  @Override
  public <O> O maybeGet(Class<O> type) {
    return maybeGet(TypeToken.of(type));
  }

  @Nullable
  @Override
  public <O> O maybeGet(TypeToken<O> type) {
    if (type.isAssignableFrom(entry.getType())) {
      @SuppressWarnings("unchecked") O cast = (O) entry.get();
      return cast;
    } else {
      return null;
    }
  }

  @Override
  public <O> List<O> getAll(Class<O> type) {
    return getAll(TypeToken.of(type));
  }

  @Override
  public <O> List<O> getAll(TypeToken<O> type) {
    O value = maybeGet(type);
    if (value == null) {
      return ImmutableList.of();
    } else {
      return ImmutableList.of(value);
    }
  }

  @Nullable
  @Override
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    T value = maybeGet(type);
    if (value != null && predicate.apply(value)) {
      return value;
    } else {
      return null;
    }
  }

  @Override
  public <T> List<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    T value = first(type, predicate);
    if (value != null) {
      return ImmutableList.of(value);
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    T first = first(type, predicate);
    if (first == null) {
      return false;
    } else {
      action.execute(first);
      return true;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingleEntryRegistry that = (SingleEntryRegistry) o;

    return entry.equals(that.entry);
  }

  @Override
  public int hashCode() {
    return entry.hashCode();
  }
}
