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
 * A builder of {@link Registry registries}.
 * <p>
 * For create single entry registries, see the factory methods on {@link Registry}.
 * A builder can be used for creating an registry with multiple entries.
 *
 * @see Registry#builder()
 */
public interface RegistryBuilder extends RegistrySpec {

  /**
   * How many entries have been added so far.
   *
   * @return how many entries have been added so far.
   */
  int size();

  /**
   * {@inheritDoc}
   */
  @Override
  default RegistryBuilder add(Object object) {
    RegistrySpec.super.add(object);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> RegistryBuilder add(Class<O> type, O object) {
    return add(Types.token(type), object);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> RegistryBuilder add(TypeToken<O> type, O object) {
    return addLazy(type, () -> object);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> RegistryBuilder addLazy(Class<O> type, Supplier<? extends O> supplier) {
    return addLazy(Types.token(type), supplier);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  <O> RegistryBuilder addLazy(TypeToken<O> type, Supplier<? extends O> supplier);

  @Override
  default RegistryBuilder with(Action<? super RegistrySpec> action) throws Exception {
    RegistrySpec.super.with(action);
    return this;
  }

  /**
   * Builds the registry.
   *
   * @return a newly created registry
   */
  Registry build();

}
