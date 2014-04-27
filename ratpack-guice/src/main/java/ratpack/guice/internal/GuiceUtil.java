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
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import ratpack.func.Action;
import ratpack.func.Transformer;

import java.util.Map;

import static ratpack.util.ExceptionUtils.uncheck;

public abstract class GuiceUtil {

  private GuiceUtil() {
  }

  public static <T> void search(Injector injector, TypeToken<T> type, Transformer<T, Boolean> transformer) {
    Map<Key<?>, Binding<?>> allBindings = injector.getAllBindings();
    for (Map.Entry<Key<?>, Binding<?>> keyBindingEntry : allBindings.entrySet()) {
      TypeLiteral<?> bindingType = keyBindingEntry.getKey().getTypeLiteral();
      if (type.isAssignableFrom(toTypeToken(bindingType))) {
        @SuppressWarnings("unchecked")
        T thing = (T) keyBindingEntry.getValue().getProvider().get();
        try {
          if (!transformer.transform(thing)) {
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
    search(injector, type, new Transformer<T, Boolean>() {
      @Override
      public Boolean transform(T from) throws Exception {
        action.execute(from);
        return true;
      }
    });
  }

  public static <T> ImmutableList<T> allOfType(Injector injector, TypeToken<T> type) {
    final ImmutableList.Builder<T> listBuilder = ImmutableList.builder();
    search(injector, type, new Transformer<T, Boolean>() {
      @Override
      public Boolean transform(T from) throws Exception {
        listBuilder.add(from);
        return true;
      }
    });
    return listBuilder.build();
  }

  public static <T> T firstOfType(Injector injector, TypeToken<T> type) {
    Catcher<T> catcher = new Catcher<>();
    search(injector, type, catcher);
    return catcher.value;
  }

  public static <T> TypeToken<T> toTypeToken(TypeLiteral<T> type) {
    @SuppressWarnings("unchecked") TypeToken<T> typeToken = (TypeToken<T>) TypeToken.of(type.getType());
    return typeToken;
  }

  private static class Catcher<T> implements Transformer<T, Boolean> {
    public T value;

    @Override
    public Boolean transform(T from) throws Exception {
      value = from;
      return false;
    }
  }
}
