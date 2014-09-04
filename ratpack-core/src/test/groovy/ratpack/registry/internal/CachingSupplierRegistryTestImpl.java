/*
 * Copyright 2014 the original author or authors.
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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.TypeToken;
import ratpack.func.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Subclass of CachingSupplierRegistry for testing CachingSupplierRegistry
 */
public class CachingSupplierRegistryTestImpl extends CachingSupplierRegistry {
  final List<Pair<TypeToken<?>, ? extends Supplier<?>>> supplierEntries;

  public CachingSupplierRegistryTestImpl() {
    this(new LinkedList<Pair<TypeToken<?>, ? extends Supplier<?>>>());
  }

  public CachingSupplierRegistryTestImpl(final List<Pair<TypeToken<?>, ? extends Supplier<?>>> supplierEntries) {
    super(new Function<TypeToken<?>, List<? extends Supplier<?>>>() {
      @Override
      public List<? extends Supplier<?>> apply(final TypeToken<?> typeToken) {
        return FluentIterable.from(supplierEntries).filter(new Predicate<Pair<TypeToken<?>, ? extends Supplier<?>>>() {
          @Override
          public boolean apply(Pair<TypeToken<?>, ? extends Supplier<?>> entry) {
            return typeToken.isAssignableFrom(entry.getLeft());
          }
        }).transform(new Function<Pair<TypeToken<?>, ? extends Supplier<?>>, Supplier<?>>() {
          @Override
          public Supplier<?> apply(Pair<TypeToken<?>, ? extends Supplier<?>> input) {
            return input.getRight();
          }
        }).toList();
      }
    });
    this.supplierEntries = supplierEntries;
  }

  public void register(Object instance) {
    register(TypeToken.of(instance.getClass()), instance);
  }

  public void register(TypeToken<?> type, Object instance) {
    register(type, Suppliers.ofInstance(instance));
  }

  public void register(TypeToken<?> type, Supplier<?> supplier) {
    supplierEntries.add(Pair.<TypeToken<?>, Supplier<?>>of(type, supplier));
  }
}