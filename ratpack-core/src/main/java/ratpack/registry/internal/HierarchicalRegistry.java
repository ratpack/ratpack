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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.registry.Registry;

public class HierarchicalRegistry implements Registry {

  private final Registry parent;
  private final Registry child;

  public HierarchicalRegistry(Registry parent, Registry child) {
    this.parent = parent;
    this.child = child;
  }

  @Nullable
  @Override
  public <O> O maybeGet(TypeToken<O> type) {
    O object = child.maybeGet(type);
    if (object == null) {
      object = parent.maybeGet(type);
    }

    return object;
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    Iterable<? extends O> childAll = child.getAll(type);
    Iterable<? extends O> parentAll = parent.getAll(type);
    return Iterables.concat(childAll, parentAll);
  }

  @Nullable
  @Override
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    T first = child.first(type, predicate);
    if (first == null) {
      first = parent.first(type, predicate);
    }
    return first;
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    Iterable<? extends T> childAll = child.all(type, predicate);
    Iterable<? extends T> parentAll = parent.all(type, predicate);
    return Iterables.concat(childAll, parentAll);
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    boolean childFound = child.each(type, predicate, action);
    boolean parentFound = parent.each(type, predicate, action);
    return childFound || parentFound;
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
