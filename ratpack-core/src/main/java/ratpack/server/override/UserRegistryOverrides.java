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

package ratpack.server.override;

import ratpack.func.Function;
import ratpack.registry.Registry;

public final class UserRegistryOverrides {

  private final Function<? super Registry, ? extends Registry> registryFunc;

  private UserRegistryOverrides(Function<? super Registry, ? extends Registry> registryFunc) {
    this.registryFunc = registryFunc;
  }

  public static UserRegistryOverrides none() {
    return of(Registry.empty());
  }

  public static UserRegistryOverrides of(Registry registry) {
    return of(Function.constant(registry));
  }

  public static UserRegistryOverrides of(Function<? super Registry, ? extends Registry> registry) {
    return new UserRegistryOverrides(registry);
  }

  public Registry build(Registry input) throws Exception {
    return registryFunc.apply(input);
  }
}
