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

package ratpack.registry;

import ratpack.registry.internal.DefaultRegistryBuilder;
import ratpack.registry.internal.HierarchicalRegistry;
import ratpack.util.Factory;

/**
 * Static methods for creating and building {@link ratpack.registry.Registry registries}.
 */
public abstract class Registries {

  private Registries() {
  }

  public static <T> Registry registry(Class<? super T> publicType, T implementation) {
    return builder().add(publicType, implementation).build();
  }

  public static <T> Registry registry(Class<T> publicType, Factory<? extends T> factory) {
    return builder().add(publicType, factory).build();
  }

  public static Registry registry(Object object) {
    return builder().add(object).build();
  }

  public static RegistryBuilder builder() {
    return new DefaultRegistryBuilder();
  }

  public static Registry join(Registry parent, Registry child) {
    return new HierarchicalRegistry(parent, child);
  }

}
