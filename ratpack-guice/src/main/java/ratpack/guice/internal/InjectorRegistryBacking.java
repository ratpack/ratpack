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

package ratpack.guice.internal;

import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.inject.Injector;
import com.google.inject.Provider;
import ratpack.registry.RegistryBacking;

public class InjectorRegistryBacking implements RegistryBacking {
  private final Injector injector;

  public InjectorRegistryBacking(Injector injector) {
    this.injector = injector;
  }

  @Override
  public <T> Iterable<Supplier<? extends T>> provide(TypeToken<T> type) {
    ImmutableList<Provider<? extends T>> providers = GuiceUtil.allProvidersOfType(injector, type);
    return FluentIterable
      .from(providers.reverse())
      .transform(provider -> provider::get);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InjectorRegistryBacking that = (InjectorRegistryBacking) o;

    return injector.equals(that.injector);
  }

  @Override
  public int hashCode() {
    return injector.hashCode();
  }
}
