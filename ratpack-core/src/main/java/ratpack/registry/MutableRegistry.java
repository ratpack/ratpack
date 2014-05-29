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

package ratpack.registry;

import ratpack.func.Factory;

/**
 * A {@link Registry} that is also mutable.
 */
public interface MutableRegistry extends Registry {

  /**
   * Register the given object under the given type.
   *
   * @param type The public type of the object
   * @param object The object to add to the registry
   * @param <T> The public type of the object
   */
  <T> void register(Class<T> type, T object);

  /**
   * Registers the given object with its concrete type.
   *
   * @param object The object to register
   */
  void register(Object object);

  /**
   * Registers a lazily created entry to the registry.
   * <p>
   * The factory will be invoked exactly once, when a query is made to the registry of a compatible type of the given type.
   *
   * @param type the public type of the registry entry
   * @param factory the factory for creating the object when needed
   * @param <T> the public type of the registry entry
   */
  <T> void registerLazy(Class<T> type, Factory<? extends T> factory);

  /**
   * Remove the registration for the given type.
   *
   * @param type The type of the thing to remove
   * @param <T> The type of the thing to remove
   * @throws NotInRegistryException if there is nothing registered by that type
   */
  <T> void remove(Class<T> type) throws NotInRegistryException;

}
