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

package ratpack.render.internal;

import ratpack.handling.Context;
import ratpack.render.Renderer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class ByTypeRenderer<T> implements Renderer<T> {

  private final Class<T> type;

  protected ByTypeRenderer() {
    @SuppressWarnings("unchecked")
    Class<T> renderedType = (Class<T>) findRenderedType(getClass());
    this.type = renderedType;
  }

  @Override
  public Class<T> getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  abstract public void render(Context context, T object);

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
}
