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

import ratpack.api.Nullable;

import java.util.List;

/**
 * An object that can potentially provide objects of given types.
 *
 * A registry acts as a kind of service locator. A registry can be requested to provide an object of a certain type.
 * <p>
 * Registry objects must be threadsafe.
 */
public interface Registry {

  /**
   * Provides an object of the specified type, or throws an exception if no object of that type is available.
   *
   * @param type The type of the object to provide
   * @param <O> The type of the object to provide
   * @return An object of the specified type
   * @throws NotInRegistryException If no object of this type can be returned
   */
  <O> O get(Class<O> type) throws NotInRegistryException;

  /**
   * Does the same thing as {@link #get(Class)}, except returns null instead of throwing an exception.
   *
   * @param type The type of the object to provide
   * @param <O> The type of the object to provide
   * @return An object of the specified type, or null if no object of this type is available.
   */
  @Nullable
  <O> O maybeGet(Class<O> type);

  /**
   * Returns all of the objects whose declared type is assignment compatible with the given type.
   *
   * @param type the type of objects to search for
   * @param <O> the type of objects to search for
   * @return All objects of the given type
   */
  <O> List<O> getAll(Class<O> type);

  /**
   * Does this registry contain exactly no contents.
   * <p>
   * Implementations may return true if they cannot determine if they are empty or not.
   * That is, this method may return true and yet all of its methods not return anything.
   * However, implementations that do return true from this method MUST NOT return anything from lookup methods.
   *
   * @return true if this registry contains exactly no contents, otherwise false.
   */
  boolean isEmpty();

}
