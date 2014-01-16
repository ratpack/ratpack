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

package ratpack.remote.internal;

import ratpack.api.Nullable;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.remote.CommandDelegate;
import ratpack.util.Factory;

import java.util.List;

public abstract class DelegatingCommandDelegate implements CommandDelegate {

  private final RegistrySpec spec;
  private final Registry registry;

  public DelegatingCommandDelegate(RegistrySpec spec, Registry registry) {
    this.spec = spec;
    this.registry = registry;
  }

  public <O> RegistrySpec add(Class<? super O> type, O object) {
    return spec.add(type, object);
  }

  public <O> RegistrySpec add(Class<O> type, Factory<? extends O> object) {
    return spec.add(type, object);
  }

  public <O> RegistrySpec add(O object) {
    return spec.add(object);
  }

  public <O> O get(Class<O> type) throws NotInRegistryException {
    return registry.get(type);
  }

  public <O> List<O> getAll(Class<O> type) {
    return registry.getAll(type);
  }

  public boolean isEmpty() {
    return registry.isEmpty();
  }

  @Nullable
  public <O> O maybeGet(Class<O> type) {
    return registry.maybeGet(type);
  }

}
