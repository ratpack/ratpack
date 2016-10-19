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

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.inject.*;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.registry.internal.TypeCaching;
import ratpack.util.Types;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static ratpack.util.Exceptions.uncheck;

public abstract class GuiceUtil {

  private GuiceUtil() {
  }

  public static <T> void search(Injector injector, TypeToken<T> type, Function<Provider<? extends T>, Boolean> transformer) {
    ConcurrentMap<TypeToken<?>, Boolean> cache = TypeCaching.cache(type);
    Map<Key<?>, Binding<?>> bindings = injector.getBindings();
    for (Map.Entry<Key<?>, Binding<?>> keyBindingEntry : bindings.entrySet()) {
      final Key<?> key = keyBindingEntry.getKey();
      final Binding<?> binding = keyBindingEntry.getValue();
      TypeLiteral<?> bindingType = key.getTypeLiteral();
      if (TypeCaching.isAssignableFrom(cache, type, toTypeToken(bindingType))) {
        @SuppressWarnings("unchecked") Provider<? extends T> provider = (Provider<? extends T>) binding.getProvider();
        try {
          if (!transformer.apply(provider)) {
            return;
          }
        } catch (Exception e) {
          throw uncheck(e);
        }
      }
    }
    Injector parent = injector.getParent();
    if (parent != null) {
      search(parent, type, transformer);
    }
  }

  public static <T> void eachOfType(Injector injector, TypeToken<T> type, final Action<? super T> action) {
    search(injector, type, from -> {
      action.execute(from.get());
      return true;
    });
  }

  public static <T> void eachProviderOfType(Injector injector, TypeToken<T> type, final Action<? super Provider<? extends T>> action) {
    search(injector, type, from -> {
      action.execute(from);
      return true;
    });
  }

  public static <T> ImmutableList<Provider<? extends T>> allProvidersOfType(Injector injector, TypeToken<T> type) {
    final ImmutableList.Builder<Provider<? extends T>> listBuilder = ImmutableList.builder();
    eachProviderOfType(injector, type, listBuilder::add);
    return listBuilder.build();
  }

  public static <T> TypeToken<T> toTypeToken(TypeLiteral<T> type) {
    return TypeCaching.typeToken(type.getType());
  }

  public static <T> TypeLiteral<T> toTypeLiteral(TypeToken<T> type) {
    return Types.cast(TypeLiteral.get(type.getType()));
  }

}
