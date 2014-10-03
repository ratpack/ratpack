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

import ratpack.func.Action;
import ratpack.registry.internal.CachingBackedRegistry;
import ratpack.registry.internal.DefaultRegistryBuilder;

import java.util.function.Supplier;

/**
 * Static methods for creating and building {@link ratpack.registry.Registry registries}.
 */
public abstract class Registries {

  private Registries() {
  }

  /**
   * Creates a single lazily created entry registry, using {@link RegistryBuilder#addLazy(Class, Supplier)}.
   *
   * @param publicType the public type of the entry
   * @param supplier the supplier for the object
   * @param <T> the public type of the entry
   * @return a new single entry registry
   * @see RegistryBuilder#addLazy(Class, Supplier)
   */
  public static <T> Registry just(Class<T> publicType, Supplier<? extends T> supplier) {
    return registry().addLazy(publicType, supplier).build();
  }

  /**
   * Creates a single entry registry, using {@link RegistryBuilder#add(Object)}.
   *
   * @param object the entry object
   * @return a new single entry registry
   * @see RegistryBuilder#add(java.lang.Object)
   */
  public static Registry just(Object object) {
    return registry().add(object).build();
  }

  /**
   * Creates a single entry registry, using {@link RegistryBuilder#add(Class, Object)}.
   *
   * @param publicType the public type of the entry
   * @param implementation the entry object
   * @param <T> the public type of the entry
   * @return a new single entry registry
   * @see RegistryBuilder#add(Class, Object)
   */
  public static <T> Registry just(Class<? super T> publicType, T implementation) {
    return registry().add(publicType, implementation).build();
  }

  /**
   * Creates a new {@link RegistryBuilder registry builder}.
   *
   * @return a new registry builder
   * @see RegistryBuilder
   */
  public static RegistryBuilder registry() {
    return new DefaultRegistryBuilder();
  }

  /**
   * Builds a registry from the given action.
   *
   * @param action the action that defines the registry
   * @return a registry created by the given action
   * @throws Exception any thrown by the action
   */
  public static Registry registry(Action<? super RegistrySpec> action) throws Exception {
    RegistryBuilder builder = Registries.registry();
    action.execute(builder);
    return builder.build();
  }

  /**
   * Creates a new registry instance that is backed by a RegistryBacking implementation.
   * The registry instance caches lookups.
   *
   * @param registryBacking the implementation that returns instances for the registry
   * @return a new registry
   */
  public static Registry backedRegistry(final RegistryBacking registryBacking) {
    return new CachingBackedRegistry(registryBacking);
  }
}
