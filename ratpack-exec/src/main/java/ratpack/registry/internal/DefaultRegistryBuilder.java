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

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;

import java.util.function.Supplier;

public class DefaultRegistryBuilder implements RegistryBuilder {

  private final ImmutableList.Builder<RegistryEntry<?>> builder = ImmutableList.builder();
  private int size;

  @Override
  public int size() {
    return size;
  }

  @Override
  public <O> RegistryBuilder add(TypeToken<O> type, O object) {
    builder.add(new DefaultRegistryEntry<>(type, object));
    ++size;
    return this;
  }

  @Override
  public <O> RegistryBuilder addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    builder.add(new LazyRegistryEntry<>(type, supplier));
    ++size;
    return this;
  }

  @Override
  public Registry build() {
    ImmutableList<RegistryEntry<?>> entries = builder.build();
    if (entries.size() == 1) {
      return new SingleEntryRegistry(entries.get(0));
    } else {
      return CachingRegistry.of(new MultiEntryRegistry(entries.reverse()));
    }
  }

}
