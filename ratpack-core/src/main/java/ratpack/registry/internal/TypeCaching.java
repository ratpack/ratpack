/*
 * Copyright 2015 the original author or authors.
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
import ratpack.server.internal.ServerEnvironment;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public abstract class TypeCaching {

  private static class Impl {
    boolean isAssignableFrom(TypeToken<?> left, TypeToken<?> right) {
      return left.isAssignableFrom(right);
    }

    @SuppressWarnings("unchecked")
    <T> TypeToken<T> typeToken(Type type) {
      return (TypeToken<T>) TypeToken.of(type);
    }
  }

  private static class CachingImpl extends Impl {

    private final ConcurrentMap<TypeToken<?>, ConcurrentMap<TypeToken<?>, Boolean>> assignabilityCache = new ConcurrentHashMap<>();
    private final Function<TypeToken<?>, ConcurrentMap<TypeToken<?>, Boolean>> assignabilityIndexProducer = t -> new ConcurrentHashMap<>();

    private final ConcurrentMap<Type, TypeToken<?>> typeTokensCache = new ConcurrentHashMap<>();
    private final Function<Type, TypeToken<?>> typeTokenProducer = TypeToken::of;

    boolean isAssignableFrom(TypeToken<?> left, TypeToken<?> right) {
      ConcurrentMap<TypeToken<?>, Boolean> forLeft = assignabilityCache.get(left);
      if (forLeft == null) {
        forLeft = assignabilityCache.computeIfAbsent(left, assignabilityIndexProducer);
      }
      Boolean value = forLeft.get(right);
      if (value == null) {
        boolean assignableFrom = left.isAssignableFrom(right);
        forLeft.put(right, assignableFrom);
        return assignableFrom;
      }
      return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    <T> TypeToken<T> typeToken(Type type) {
      TypeToken<?> typeToken = typeTokensCache.get(type);
      if (typeToken == null) {
        return (TypeToken<T>) typeTokensCache.computeIfAbsent(type, typeTokenProducer);
      } else {
        return (TypeToken<T>) typeToken;
      }
    }
  }

  private static final Impl IMPL = ServerEnvironment.INSTANCE.isDevelopment() ? new Impl() : new CachingImpl();

  public static boolean isAssignableFrom(TypeToken<?> left, TypeToken<?> right) {
    return IMPL.isAssignableFrom(left, right);
  }

  public static <T> TypeToken<T> typeToken(Type type) {
    return IMPL.typeToken(type);
  }


}
