/*
 * Copyright 2016 the original author or authors.
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

import com.google.common.reflect.TypeToken;
import ratpack.registry.MutableRegistry;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

import java.util.function.Supplier;

public class HierarchicalMutableRegistry extends HierarchicalRegistry implements MutableRegistry {

  private final Registry parent;
  private final MutableRegistry child;

  public HierarchicalMutableRegistry(Registry parent, MutableRegistry child) {
    super(parent, child);
    this.parent = parent;
    this.child = child;
  }

  @Override
  public <T> void remove(TypeToken<T> type) throws NotInRegistryException {
    child.remove(type);
  }

  @Override
  public Registry asImmutable() {
    return parent.join(child.asImmutable());
  }

  @Override
  public <O> RegistrySpec addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    return child.addLazy(type, supplier);
  }
}
