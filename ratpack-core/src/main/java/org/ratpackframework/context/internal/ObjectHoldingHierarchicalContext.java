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

package org.ratpackframework.context.internal;

import org.ratpackframework.context.Context;

/**
 * A simple context that
 */
public class ObjectHoldingHierarchicalContext extends HierarchicalContextSupport {

  private final Object value;
  private final Class<?> type;

  @SuppressWarnings("unchecked")
  public ObjectHoldingHierarchicalContext(Context parent, Object value) {
    this(parent, (Class<? super Object>) value.getClass(), value);
  }

  public <T> ObjectHoldingHierarchicalContext(Context parent, Class<? super T> type, T value) {
    super(parent);
    this.value = value;
    this.type = type;
  }

  @Override
  protected <T> T doMaybeGet(Class<T> targetType) {
    if (targetType.equals(type)) {
      return targetType.cast(value);
    } else {
      return null;
    }
  }

  @Override
  protected String describe() {
    return "ObjectContext{" + value + "}";
  }
}
