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

package ratpack.guice.internal;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.inject.Injector;
import com.google.inject.Provider;
import ratpack.registry.internal.CachingSupplierRegistry;

import java.util.List;

public class InjectorBackedRegistry extends CachingSupplierRegistry {
  final Injector injector;

  public InjectorBackedRegistry(final Injector injector) {
    super(new Function<TypeToken<?>, List<? extends Supplier<?>>>() {
      @Override
      public List<? extends Supplier<?>> apply(TypeToken<?> input) {
        return Lists.transform(GuiceUtil.allProvidersOfType(injector, input), new Function<Provider<?>, Supplier<?>>() {
          @Override
          public Supplier<?> apply(final Provider<?> provider) {
            return new Supplier<Object>() {
              @Override
              public Object get() {
                return provider.get();
              }
            };
          }
        });
      }
    });
    this.injector = injector;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InjectorBackedRegistry that = (InjectorBackedRegistry) o;

    return injector.equals(that.injector);
  }

  @Override
  public int hashCode() {
    return injector.hashCode();
  }
}
