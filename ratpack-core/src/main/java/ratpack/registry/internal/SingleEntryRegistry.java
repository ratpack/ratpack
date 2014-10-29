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
import ratpack.func.Action;
import ratpack.registry.Registry;

import java.util.Collections;
import java.util.Optional;

public class SingleEntryRegistry implements Registry {

  private final RegistryEntry<?> entry;

  public SingleEntryRegistry(RegistryEntry<?> entry) {
    this.entry = entry;
  }

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    if (type.isAssignableFrom(entry.getType())) {
      @SuppressWarnings("unchecked") O cast = (O) entry.get();
      return Optional.of(cast);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return maybeGet(type).map(Collections::<O>singleton).orElse(Collections.emptySet());
  }

  @Override
  public <T> Optional<T> first(TypeToken<T> type, Predicate<? super T> predicate) {
    return maybeGet(type).filter(predicate::apply);
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    return first(type, predicate).map(ImmutableList::of).orElse(ImmutableList.of());
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    Optional<T> first = first(type, predicate);
    if (!first.isPresent()) {
      return false;
    } else {
      action.execute(first.get());
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

  @Override
  public String toString() {
    return "SingleEntryRegistry{" + entry + '}';
  }
}
