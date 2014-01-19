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

import ratpack.util.Factory;

/**
 * A builder of {@link Registry registries}.
 *
 * @see Registries#builder()
 */
public interface RegistryBuilder extends RegistrySpec {

  /**
   * {@inheritDoc}
   */
  @Override
  <O> RegistryBuilder add(Class<? super O> type, O object);

  /**
   * {@inheritDoc}
   */
  @Override
  RegistryBuilder add(Object object);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> RegistryBuilder add(Class<O> type, Factory<? extends O> factory);

  /**
   * Builds the registry.
   *
   * @return a newly created registry
   */
  Registry build();

  /**
   * Builds a registry containing the entries specified by this builder and the given “parent” registry.
   * <p>
   * Entries added in this builder will shadow items in the given parent.
   *
   * @param parent the parent of the registry to create
   * @return a newly created registry
   */
  Registry build(Registry parent);

}
