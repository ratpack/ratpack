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
import org.ratpackframework.registry.NotInRegistryException;
import org.ratpackframework.registry.Registry;

import java.util.List;

public class DefaultRegistry implements Registry {

  private final ImmutableList<RegistryEntry<?>> entries;

  public DefaultRegistry(ImmutableList<RegistryEntry<?>> entries) {
    this.entries = entries;
  }

  @Override
  public String toString() {
    return "Registry{" + entries + '}';
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    O object = maybeGet(type);
    if (object == null) {
      throw new NotInRegistryException(type);
    } else {
      return null;
    }
  }

  public <O> O maybeGet(Class<O> type) {
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        return type.cast(entry.get());
      }
    }

    return null;
  }

  public <O> List<O> getAll(Class<O> type) {
    ImmutableList.Builder<O> builder = ImmutableList.builder();
    for (RegistryEntry<?> entry : entries) {
      if (type.isAssignableFrom(entry.getType())) {
        builder.add(type.cast(entry.get()));
      }
    }
    return builder.build();
  }


}
