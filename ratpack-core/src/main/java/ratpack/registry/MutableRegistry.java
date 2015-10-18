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

import com.google.common.reflect.TypeToken;
import ratpack.util.Types;

/**
 * A {@link Registry} that is also mutable.
 */
public interface MutableRegistry extends Registry, RegistrySpec {

  /**
   * Remove the registration for the given type.
   *
   * @param type The type of the thing to remove
   * @param <T> The type of the thing to remove
   * @throws NotInRegistryException if there is nothing registered by that type
   */
  default <T> void remove(Class<T> type) throws NotInRegistryException {
    remove(Types.token(type));
  }

  /**
   * Remove the registration for the given type.
   *
   * @param type The type of the thing to remove
   * @param <T> The type of the thing to remove
   * @throws NotInRegistryException if there is nothing registered by that type
   */
  <T> void remove(TypeToken<T> type) throws NotInRegistryException;

}
