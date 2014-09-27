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

package ratpack.remote.internal;

import com.google.common.base.Predicate;
import com.google.common.reflect.TypeToken;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.remote.CommandDelegate;

import java.util.function.Supplier;

public abstract class DelegatingCommandDelegate implements CommandDelegate {

  private final RegistrySpec spec;
  private final Registry registry;

  public DelegatingCommandDelegate(RegistrySpec spec, Registry registry) {
    this.spec = spec;
    this.registry = registry;
  }

  @Override
  public <O> RegistrySpec add(TypeToken<? super O> type, O object) {
    return spec.add(type, object);
  }

  @Override
  public <O> RegistrySpec addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    return spec.addLazy(type, supplier);
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
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.first(type, predicate);
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.all(type, predicate);
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    return registry.each(type, predicate, action);
  }
}
