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

package org.ratpackframework.render;

import org.ratpackframework.api.TypeLiteral;
import org.ratpackframework.handling.Context;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A convenience super class for renderers that can only render one type of object.
 *
 * @param <T> The type of object that can be rendered.
 */
public abstract class ByTypeRenderer<T> implements Renderer<T> {

  private final Class<T> type;

  private static Class<?> findRenderedType(Class<?> type) {
    ParameterizedType renderInterfaceType = findRenderSuperclassType(type.getGenericSuperclass());
    Type[] actualTypeArguments = renderInterfaceType.getActualTypeArguments();
    Type tType = actualTypeArguments[0];

    if (tType instanceof Class) {
      return (Class<?>) tType;
    } else if (tType instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) tType).getRawType();
    } else {
      throw new IllegalStateException("Class " + type.getName() + " should concretely implement Render");
    }
  }

  private static ParameterizedType findRenderSuperclassType(Type type) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Class<?> rawType = (Class<?>) parameterizedType.getRawType();
      if (rawType.equals(ByTypeRenderer.class)) {
        return parameterizedType;
      } else {
        return findRenderSuperclassType(rawType.getGenericSuperclass());
      }
    } else if (type instanceof Class) {
      Class<?> classType = (Class) type;
      if (type.equals(Object.class)) {
        throw new IllegalStateException(type + " does not extend " + ByTypeRenderer.class);
      }
      return findRenderSuperclassType(classType.getGenericSuperclass());
    } else {
      throw new IllegalStateException("Unhandled type: " + type);
    }
  }

  public ByTypeRenderer() {
    @SuppressWarnings("unchecked")
    Class<T> renderedType = (Class<T>) findRenderedType(getClass());
    this.type = renderedType;
  }

  /**
   * Use the given type as the target to-render type.
   *
   * @param type the target to-render type.
   */
  protected ByTypeRenderer(Class<T> type) {
    this.type = type;
  }

  /**
   * Use the given type as the target to-render type.
   * <p>
   * Useful if the target type is a generic type.
   *
   * @param typeLiteral the target to-render type.
   */
  protected ByTypeRenderer(TypeLiteral<T> typeLiteral) {
    @SuppressWarnings("unchecked") Class<T> rawType = (Class<T>) typeLiteral.getRawType();
    this.type = rawType;
  }

  /**
   * Accepts the object if it is an instance of {@code T}.
   *
   * @param object An object to potentially accept to render.
   * @return The same input object, cast to {@code T}.
   */

  public T accept(Object object) {
    if (type.isInstance(object)) {
      return type.cast(object);
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  abstract public void render(Context context, T object);
}
