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

package ratpack.groovy.handling.internal;

import ratpack.exec.api.Nullable;
import ratpack.exec.func.Function;
import ratpack.groovy.handling.GroovyChain;
import ratpack.core.handling.Handler;
import ratpack.core.handling.internal.DefaultChain;
import ratpack.core.server.ServerConfig;
import ratpack.exec.registry.Registry;

import java.util.List;

public class GroovyDslChainActionTransformer implements Function<List<Handler>, GroovyChain> {

  private final ServerConfig serverConfig;
  private final Registry registry;

  public GroovyDslChainActionTransformer(ServerConfig serverConfig, @Nullable Registry registry) {
    this.serverConfig = serverConfig;
    this.registry = registry;
  }

  public GroovyChain apply(List<Handler> storage) {
    return new DefaultGroovyChain(new DefaultChain(storage, serverConfig, registry));
  }

}
