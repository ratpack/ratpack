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

package org.ratpackframework.guice.internal;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.registry.internal.HierarchicalRegistrySupport;

public class InjectorBackedHierarchicalRegistry extends HierarchicalRegistrySupport {

  final Injector injector;

  public InjectorBackedHierarchicalRegistry(Registry parent, Injector injector) {
    super(parent);
    this.injector = injector;
  }

  @Override
  protected <T> T doMaybeGet(Class<T> type) {
    Binding<T> existingBinding = injector.getExistingBinding(Key.get(type));
    if (existingBinding == null) {
      return null;
    } else {
      return existingBinding.getProvider().get();
    }
  }

  @Override
  protected String describe() {
    return "Injector";
  }

}
