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
import org.ratpackframework.util.Factory;

public class LazyHierarchicalContext extends HierarchicalContextSupport {

  private final Class<?> type;
  private final Factory<?> factory;
  private Object object;

  public <T> LazyHierarchicalContext(Context parent, Class<T> type, Factory<? extends T> factory) {
    super(parent);
    this.type = type;
    this.factory = factory;
  }

  @Override
  protected <T> T doMaybeGet(Class<T> requestedType) {
    if (type.isAssignableFrom(requestedType)) {
      return requestedType.cast(getObject());
    } else {
      return  null;
    }
  }

  private Object getObject() {
    if (object == null) {
      object = factory.create();
    }
    return object;
  }

  @Override
  protected String describe() {
    return "LazyContext{" + type.getName() + "}";
  }
}
