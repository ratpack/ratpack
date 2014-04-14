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

import ratpack.func.Action;
import ratpack.func.Factory;

public abstract class RegistrySpecAction implements Action<RegistrySpec>, RegistrySpec {

  private RegistrySpec registrySpec;

  protected RegistrySpec getRegistrySpec() throws IllegalStateException {
    if (registrySpec == null) {
      throw new IllegalStateException("no registry spec set - RegistrySpec methods should only be called during execute()");
    }
    return registrySpec;
  }

  /**
   * Delegates to {@link #execute()}, using the given {@code registrySpec} for delegation.
   *
   * @param registrySpec the registry spec
   * @throws Exception Any thrown by {@link #execute()}
   */
  public final void execute(RegistrySpec registrySpec) throws Exception {
    try {
      this.registrySpec = registrySpec;
      execute();
    } finally {
      this.registrySpec = null;
    }
  }

  /**
   * Implementations can naturally use the {@link RegistrySpec} DSL for the duration of this method.
   *
   * @throws Exception Any exception thrown while adding to the registry
   */
  protected abstract void execute() throws Exception;

  @Override
  public <O> RegistrySpec add(Class<? super O> type, O object) {
    return getRegistrySpec().add(type, object);
  }

  @Override
  public RegistrySpec add(Object object) {
    return getRegistrySpec().add(object);
  }

  @Override
  public <O> RegistrySpec add(Class<O> type, Factory<? extends O> factory) {
    return getRegistrySpec().add(type, factory);
  }

}
