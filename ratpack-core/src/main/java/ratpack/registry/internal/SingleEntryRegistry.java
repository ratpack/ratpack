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
import ratpack.func.Function;
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
    if (TypeCaching.isAssignableFrom(TypeCaching.cache(type), type, entry.getType())) {
      @SuppressWarnings("unchecked") O cast = (O) entry.get();
      return Optional.of(cast);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    //noinspection Convert2MethodRef
    return maybeGet(type).map((o) -> Collections.singleton(o)).orElse(Collections.emptySet());
  }

  @Override
  public <T, O> Optional<O> first(TypeToken<T> type, Function<? super T, ? extends O> function) throws Exception {
    Optional<T> item = maybeGet(type);
    if (item.isPresent()) {
      return Optional.ofNullable(function.apply(item.get()));
    } else {
      return Optional.empty();
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
