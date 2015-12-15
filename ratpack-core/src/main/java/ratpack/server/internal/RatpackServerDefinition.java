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
import ratpack.handling.Handlers;
import ratpack.registry.Registry;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;

import java.util.Optional;

public final class RatpackServerDefinition {

  private final ServerConfig serverConfig;
  private final Function<? super Registry, ? extends Registry> registry;
  private final Function<? super Registry, ? extends Handler> handler;

  private RatpackServerDefinition(ServerConfig serverConfig, Function<? super Registry, ? extends Registry> registry, Function<? super Registry, ? extends Handler> handler) {
    this.serverConfig = serverConfig;
    this.registry = registry;
    this.handler = handler;
  }

  public static RatpackServerDefinition build(Action<? super RatpackServerSpec> config) throws Exception {
    SpecImpl spec = new SpecImpl();
    config.execute(spec);
    ServerConfig serverConfig = Optional.ofNullable(spec.serverConfig).orElseGet(() -> ServerConfig.builder().build());
    return new RatpackServerDefinition(serverConfig, spec.registry, spec.handler);
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  public Function<? super Registry, ? extends Registry> getRegistry() {
    return registry;
  }

  public Function<? super Registry, ? extends Handler> getHandler() {
    return handler;
  }

  private static class SpecImpl implements RatpackServerSpec {
    private ServerConfig serverConfig;
    private Function<? super Registry, ? extends Registry> registry = (r) -> Registry.empty();
    private Function<? super Registry, ? extends Handler> handler = (r) -> Handlers.notFound();

    @Override
    public RatpackServerSpec registry(Function<? super Registry, ? extends Registry> registry) {
      this.registry = registry;
      return this;
    }

    @Override
    public RatpackServerSpec serverConfig(ServerConfig serverConfig) {
      this.serverConfig = serverConfig;
      return this;
    }

    @Override
    public RatpackServerSpec handler(Function<? super Registry, ? extends Handler> handlerFactory) {
      this.handler = handlerFactory;
      return this;
    }
  }
}
