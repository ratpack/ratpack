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

import com.google.common.reflect.TypeToken;
import ratpack.func.Action;
import ratpack.util.Types;

import java.util.function.Supplier;

/**
 * An additive specification of a registry.
 *
 * @see RegistryBuilder
 */
public interface RegistrySpec {

  /**
   * Adds a registry entry that is available by the given type.
   *
   * @param type the public type of the registry entry
   * @param object the actual registry entry
   * @param <O> the public type of the registry entry
   * @return this
   */
  default <O> RegistrySpec add(Class<O> type, O object) {
    return add(Types.token(type), object);
  }

  /**
   * Adds a registry entry that is available by the given type.
   *
   * @param type the public type of the registry entry
   * @param object the actual registry entry
   * @param <O> the public type of the registry entry
   * @return this
   */
  default <O> RegistrySpec add(TypeToken<O> type, O object) {
    return addLazy(type, () -> object);
  }

  /**
   * Adds a registry entry.
   *
   * @param object the object to add to the registry
   * @return this
   */
  default RegistrySpec add(Object object) {
    @SuppressWarnings("unchecked")
    TypeToken<? super Object> type = (TypeToken<? super Object>) Types.token(object.getClass());
    return add(type, object);
  }

  /**
   * Adds a lazily created entry to the registry.
   * <p>
   * The supplier will be invoked exactly once, when a query is made to the registry of a compatible type of the given type.
   *
   * @param type the public type of the registry entry
   * @param supplier the supplier for creating the object when needed
   * @param <O> the public type of the registry entry
   * @return this
   */
  default <O> RegistrySpec addLazy(Class<O> type, Supplier<? extends O> supplier) {
    return addLazy(Types.token(type), supplier);
  }

  /**
   * Adds a lazily created entry to the registry.
   * <p>
   * The supplier will be invoked exactly once, when a query is made to the registry of a compatible type of the given type.
   *
   * @param type the public type of the registry entry
   * @param supplier the supplier for creating the object when needed
   * @param <O> the public type of the registry entry
   * @return this
   */
  <O> RegistrySpec addLazy(TypeToken<O> type, Supplier<? extends O> supplier);

  /**
   * Executes the given action with {@code this}.
   *
   * @param action the action
   * @return {@code this}
   * @throws Exception any thrown by {@code action}
   */
  default RegistrySpec with(Action<? super RegistrySpec> action) throws Exception {
    return action.with(this);
  }

}
