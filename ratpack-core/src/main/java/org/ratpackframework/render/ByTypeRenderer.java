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

/**
 * A convenience super class for renderers that can only render one type of object.
 *
 * @param <T> The type of object that can be rendered.
 */
public abstract class ByTypeRenderer<T> implements Renderer<T> {

  private final Class<T> type;

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
