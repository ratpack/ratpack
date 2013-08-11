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

public abstract class ByTypeRenderer<T> implements Renderer<T> {

  private final Class<T> type;

  protected ByTypeRenderer(Class<T> type) {
    this.type = type;
  }

  protected ByTypeRenderer(TypeLiteral<T> typeLiteral) {
    @SuppressWarnings("unchecked") Class<T> rawType = (Class<T>) typeLiteral.getRawType();
    this.type = rawType;
  }

  public T accept(Object object) {
    if (type.isInstance(object)) {
      return type.cast(object);
    } else {
      return null;
    }
  }

  abstract public void render(Context context, T object);
}
