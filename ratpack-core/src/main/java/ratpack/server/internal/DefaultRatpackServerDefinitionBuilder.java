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

package ratpack.server.internal;

import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;

public final class DefaultRatpackServerDefinitionBuilder implements RatpackServer.Definition.Builder {

  private ServerConfig serverConfig = ServerConfig.noBaseDir().build();
  private Registry userRegistry = Registries.empty();

  @Override
  public RatpackServer.Definition.Builder registry(Action<? super RegistrySpec> action) throws Exception {
    return registry(Registries.registry(action));
  }

  @Override
  public RatpackServer.Definition.Builder registry(Registry registry) {
    this.userRegistry = registry;
    return this;
  }


  @Override
  public RatpackServer.Definition.Builder config(ServerConfig serverConfig) {
    this.serverConfig = serverConfig;
    return this;
  }

  @Override
  public RatpackServer.Definition handler(Function<? super Registry, ? extends Handler> handlerFactory) {
    return new DefaultServerDefinition(serverConfig, userRegistry, handlerFactory);
  }
}
