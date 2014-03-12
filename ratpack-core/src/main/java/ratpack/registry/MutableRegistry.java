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
 *
 * @param <T> The base type of object in the registry.
 */
public interface MutableRegistry<T> extends Registry {

  /**
   * Register the given object under the given type.
   *
   * @param type The public type of the object
   * @param object The object to add to the registry
   * @param <O> The public type of the object
   */
  <O extends T> void register(Class<O> type, O object);

  /**
   * Registers the given object with its concrete type.
   *
   * @param object The object to register
   */
  void register(T object);

  /**
   * Registers a lazily created entry to the registry.
   * <p>
   * The factory will be invoked exactly once, when a query is made to the registry of a compatible type of the given type.
   *
   * @param type the public type of the registry entry
   * @param factory the factory for creating the object when needed
   * @param <O> the public type of the registry entry
   */
  <O extends T> void registerLazy(Class<O> type, Factory<? extends O> factory);

  /**
   * Remove the registration for the given type.
   *
   * @param type The type of the thing to remove
   * @param <O> The type of the thing to remove
   * @throws NotInRegistryException if there is nothing registered by that type
   */
  <O extends T> void remove(Class<O> type) throws NotInRegistryException;

}
