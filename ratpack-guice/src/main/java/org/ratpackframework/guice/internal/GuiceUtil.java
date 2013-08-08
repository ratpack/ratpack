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

package org.ratpackframework.guice.internal;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.ratpackframework.util.Action;

import java.util.Map;

public abstract class GuiceUtil {

  private GuiceUtil() {
  }

  public static <T> void eachOfType(Injector injector, TypeLiteral<T> type, Action<T> action) {
    Map<Key<?>, Binding<?>> allBindings = injector.getAllBindings();
    for (Map.Entry<Key<?>, Binding<?>> keyBindingEntry : allBindings.entrySet()) {
      Class<?> rawType = keyBindingEntry.getKey().getTypeLiteral().getRawType();
      if (type.getRawType().isAssignableFrom(rawType)) {
        @SuppressWarnings("unchecked")
        T thing = (T) keyBindingEntry.getValue().getProvider().get();
        action.execute(thing);
      }
    }
  }

  public static <T> ImmutableList<T> ofType(Injector injector, Class<T> type) {
    return ofType(injector, TypeLiteral.get(type));
  }

  // CAREFUL: Does not validate compatibility of generic types.
  public static <T> ImmutableList<T> ofType(Injector injector, TypeLiteral<T> type) {
    final ImmutableList.Builder<T> listBuilder = ImmutableList.builder();
    eachOfType(injector, type, new Action<T>() {
      public void execute(T thing) {
        listBuilder.add(thing);
      }
    });
    return listBuilder.build();
  }

}
