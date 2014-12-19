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

package ratpack.render;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import ratpack.util.Types;
import ratpack.util.internal.InternalRatpackError;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * A convenience base class for {@link RenderableDecorator} implementations.
 * <p>
 * This base class provides an implementation for the {@link #getType()} method based on the type parameter {@code T}.
 * <p>
 * Like {@link RendererSupport} implementations, the value for type variable {@code T} must be a concrete type or a parameterized type
 * with the {@code ?} wildcard for all variables due to runtime type erasure.
 *
 * @param <T> the type of object-to-render that this decorator decorates
 */
public abstract class RenderableDecoratorSupport<T> implements RenderableDecorator<T> {

  private final Class<T> type;

  protected RenderableDecoratorSupport() {
    TypeToken<T> typeToken = new TypeToken<T>(getClass()) {
    };

    Type type = typeToken.getType();
    if (type instanceof Class) {
      this.type = Types.cast(typeToken.getRawType());
    } else if (type instanceof ParameterizedType) {
      Iterable<Type> typeArgs = Arrays.asList(((ParameterizedType) type).getActualTypeArguments());
      if (Iterables.any(typeArgs, Predicates.not((t) -> t.getTypeName().equals("?")))) {
        throw new IllegalArgumentException("Invalid renderable type " + type + ": due to type erasure, type parameter T of RenderableDecorator must be a Class or a parameterized type with '?' for all type variables (e.g. List<?>)");
      }
      this.type = Types.cast(typeToken.getRawType());
    } else {
      throw new InternalRatpackError("Unhandled type for RenderableDecorator: " + type.getClass());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<T> getType() {
    return type;
  }

}
