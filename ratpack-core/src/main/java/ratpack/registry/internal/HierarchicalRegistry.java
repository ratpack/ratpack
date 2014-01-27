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

package ratpack.registry.internal;

import com.google.common.collect.ImmutableList;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;

import java.util.List;

public class HierarchicalRegistry implements Registry {

  private final Registry parent;
  private final Registry child;

  public HierarchicalRegistry(Registry parent, Registry child) {
    this.parent = parent;
    this.child = child;
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    O object = maybeGet(type);

    if (object == null) {
      throw new NotInRegistryException(type);
    } else {
      return object;
    }
  }

  @Override
  public <O> O maybeGet(Class<O> type) {
    O object = child.maybeGet(type);
    if (object == null) {
      object = parent.maybeGet(type);
    }

    return object;
  }

  @Override
  public <O> List<O> getAll(Class<O> type) {
    List<O> childAll = child.getAll(type);
    List<O> parentAll = parent.getAll(type);
    return ImmutableList.<O>builder().addAll(childAll).addAll(parentAll).build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HierarchicalRegistry that = (HierarchicalRegistry) o;

    return child.equals(that.child) && parent.equals(that.parent);
  }

  @Override
  public int hashCode() {
    int result = parent.hashCode();
    result = 31 * result + child.hashCode();
    return result;
  }
}
