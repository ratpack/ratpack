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

import java.util.Map;

import static ratpack.util.ExceptionUtils.uncheck;

public abstract class GuiceUtil {

  private GuiceUtil() {
  }

  public static <T> void search(Injector injector, TypeToken<T> type, Function<Provider<? extends T>, Boolean> transformer) {
    Map<Key<?>, Binding<?>> allBindings = injector.getAllBindings();
    for (Map.Entry<Key<?>, Binding<?>> keyBindingEntry : allBindings.entrySet()) {
      TypeLiteral<?> bindingType = keyBindingEntry.getKey().getTypeLiteral();
      if (type.isAssignableFrom(toTypeToken(bindingType))) {
        @SuppressWarnings("unchecked") Provider<? extends T> provider = (Provider<? extends T>) keyBindingEntry.getValue().getProvider();
        try {
          if (!transformer.transform(provider)) {
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
    search(injector, type, new Function<Provider<? extends T>, Boolean>() {
      @Override
      public Boolean transform(Provider<? extends T> from) throws Exception {
        action.execute(from.get());
        return true;
      }
    });
  }

  public static <T> void eachProviderOfType(Injector injector, TypeToken<T> type, final Action<? super Provider<? extends T>> action) {
    search(injector, type, new Function<Provider<? extends T>, Boolean>() {
      @Override
      public Boolean transform(Provider<? extends T> from) throws Exception {
        action.execute(from);
        return true;
      }
    });
  }

  public static <T> ImmutableList<T> allOfType(Injector injector, TypeToken<T> type) {
    final ImmutableList.Builder<T> listBuilder = ImmutableList.builder();
    eachOfType(injector, type, new Action<T>() {
      @Override
      public void execute(T thing) throws Exception {
        listBuilder.add(thing);
      }
    });
    return listBuilder.build();
  }

  public static <T> ImmutableList<Provider<? extends T>> allProvidersOfType(Injector injector, TypeToken<T> type) {
    final ImmutableList.Builder<Provider<? extends T>> listBuilder = ImmutableList.builder();
    eachProviderOfType(injector, type, new Action<Provider<? extends T>>() {
      @Override
      public void execute(Provider<? extends T> thing) throws Exception {
        listBuilder.add(thing);
      }
    });
    return listBuilder.build();
  }

  public static <T> TypeToken<T> toTypeToken(TypeLiteral<T> type) {
    @SuppressWarnings("unchecked") TypeToken<T> typeToken = (TypeToken<T>) TypeToken.of(type.getType());
    return typeToken;
  }

}
