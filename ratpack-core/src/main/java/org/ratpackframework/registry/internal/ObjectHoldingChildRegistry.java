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

package org.ratpackframework.registry.internal;

import org.ratpackframework.registry.Registry;

/**
 * A simple service that
 */
public class ObjectHoldingChildRegistry<T> extends ChildRegistrySupport<T> {

  private final T value;
  private final Class<?> type;

  @SuppressWarnings("unchecked")
  public ObjectHoldingChildRegistry(Registry<T> parent, T value) {
    this(parent, (Class<T>) value.getClass(), value);
  }

  public ObjectHoldingChildRegistry(Registry<T> parent, Class<? extends T> type, T value) {
    super(parent);
    this.value = value;
    this.type = type;
  }

  @Override
  protected <O extends T> O doMaybeGet(Class<O> targetType) {
    if (targetType.equals(type)) {
      return targetType.cast(value);
    } else {
      return null;
    }
  }

  @Override
  protected String describe() {
    return "ObjectServiceRegistry{" + value + "}";
  }
}
