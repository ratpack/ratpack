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
public class ObjectHoldingChildRegistry<T, L extends T> extends SingleValueChildRegistry<T, L> {

  private final L value;

  @SuppressWarnings("unchecked")
  public ObjectHoldingChildRegistry(Registry<T> parent, L value) {
    this(parent, (Class<L>) value.getClass(), value);
  }

  public ObjectHoldingChildRegistry(Registry<T> parent, Class<L> type, L value) {
    super(parent, type);
    this.value = value;
  }

  @Override
  protected L getObject() {
    return value;
  }

  @Override
  protected String describe() {
    return "ObjectServiceRegistry{" + value + "}";
  }
}
