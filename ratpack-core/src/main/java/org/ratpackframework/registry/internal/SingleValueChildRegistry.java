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

import com.google.common.collect.ImmutableList;
import org.ratpackframework.registry.Registry;

import java.util.List;

public abstract class SingleValueChildRegistry<T, L extends T> extends ChildRegistrySupport<T> {

  private final Class<L> type;

  public SingleValueChildRegistry(Registry<T> parent, Class<L> type) {
    super(parent);
    this.type = type;
  }

  @Override
  protected <O extends T> O doMaybeGet(Class<O> requestedType) {
    if (type.isAssignableFrom(requestedType)) {
      return requestedType.cast(getObject());
    } else {
      return null;
    }
  }

  @Override
  protected <O extends T> List<O> doChildGetAll(Class<O> type) {
    if (type.isAssignableFrom(this.type)) {
      @SuppressWarnings("unchecked") O cast = (O) getObject();
      return ImmutableList.of(cast);
    } else {
      return ImmutableList.of();
    }
  }

  protected abstract L getObject();

  protected Class<L> getType() {
    return type;
  }
}
