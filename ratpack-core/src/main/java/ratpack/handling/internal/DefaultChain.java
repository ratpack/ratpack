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

package ratpack.handling.internal;

import ratpack.api.Nullable;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;

import java.util.List;

public class DefaultChain implements Chain {

  private final List<Handler> handlers;
  private final ServerConfig serverConfig;
  private final Registry registry;

  public DefaultChain(List<Handler> handlers, ServerConfig serverConfig, @Nullable Registry registry) {
    this.handlers = handlers;
    this.serverConfig = serverConfig;
    this.registry = registry;
  }

  @Override
  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  @Override
  public Registry getRegistry() throws IllegalStateException {
    return registry;
  }

  @Override
  public Chain all(Handler handler) {
    handlers.add(handler);
    return this;
  }
}
