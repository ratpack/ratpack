/*
 * Copyright 2015 the original author or authors.
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

package ratpack.impose;

import ratpack.func.Function;
import ratpack.registry.Registry;
import ratpack.server.RatpackServerSpec;

/**
 * Imposes an extra registry to be joined with the user registry when starting an application.
 * <p>
 * If present, the imposed registry will be joined with the user registry specified by {@link RatpackServerSpec#registry(Function)}.
 * This effectively allows adding extra things to the registry.
 *
 * @see Impositions
 * @since 1.2
 */
public final class UserRegistryImposition implements Imposition {

  private final Function<? super Registry, ? extends Registry> registryFunc;

  private UserRegistryImposition(Function<? super Registry, ? extends Registry> registryFunc) {
    this.registryFunc = registryFunc;
  }

  /**
   * Creates an imposition of an empty registry.
   * <p>
   * This is equivalent to their being no imposition at all.
   *
   * @return a empty user registry imposition
   */
  public static UserRegistryImposition none() {
    return of(Registry.empty());
  }

  /**
   * Creates an imposition of the given registry.
   *
   * @param registry the registry to join with the user registry
   * @return an imposition of the given registry
   */
  public static UserRegistryImposition of(Registry registry) {
    return of(Function.constant(registry));
  }

  /**
   * Creates an imposition of registry returned by the given function.
   * <p>
   * The given function receives the user registry as input.
   * The function should not return a registry that has been joined with the input.
   * The user registry is given as input to allow retrieval from the registry.
   *
   * @param registry a function that receives the user registry and returns a registry of additions to it
   * @return an imposition of the given registry
   */
  public static UserRegistryImposition of(Function<? super Registry, ? extends Registry> registry) {
    return new UserRegistryImposition(registry);
  }

  /**
   * Returns the registry of additions, taking the original user registry as the argument
   *
   * @param userRegistry the user registry
   * @return a registry to join on to the user registry
   * @throws Exception any
   */
  public Registry build(Registry userRegistry) throws Exception {
    return registryFunc.apply(userRegistry);
  }

}
