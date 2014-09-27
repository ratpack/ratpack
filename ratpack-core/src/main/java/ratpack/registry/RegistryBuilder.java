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

import java.util.function.Supplier;

/**
 * A builder of {@link Registry registries}.
 * <p>
 * For create single entry registries, see the factory methods on {@link Registries}.
 * A builder can be used for creating an registry with multiple entries.
 *
 * @see Registries#registry()
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
  default <O> RegistryBuilder add(Class<? super O> type, O object) {
    return add(TypeToken.of(type), object);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> RegistryBuilder add(TypeToken<? super O> type, O object) {
    return addLazy(type, () -> object);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> RegistryBuilder addLazy(Class<O> type, Supplier<? extends O> supplier) {
    return addLazy(TypeToken.of(type), supplier);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  <O> RegistryBuilder addLazy(TypeToken<O> type, Supplier<? extends O> supplier);

  /**
   * Builds the registry.
   *
   * @return a newly created registry
   */
  Registry build();

  /**
   * Builds a registry containing the entries specified by this builder and the given “parent” registry.
   * <p>
   * This method uses {@link Registries#join(Registry, Registry)}, with this registry as the child argument.
   *
   * @param parent the parent of the registry to create
   * @return a newly created registry
   * @see Registries#join(Registry, Registry)
   */
  default Registry build(Registry parent) {
    return Registries.join(parent, build());
  }

}
