/*
 * Copyright 2016 the original author or authors.
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

package ratpack.groovy.internal;

import groovy.lang.Closure;
import ratpack.func.Action;
import ratpack.groovy.Groovy;
import ratpack.groovy.internal.capture.RatpackDslBacking;
import ratpack.groovy.internal.capture.RatpackDslClosures;
import ratpack.guice.Guice;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;

public class RatpackClosureConfigurer implements Action<RatpackServerSpec> {

  private final Closure<?> closure;
  private final boolean ephemeralPort;

  public RatpackClosureConfigurer(Closure<?> closure, boolean ephemeralPort) {
    this.closure = closure;
    this.ephemeralPort = ephemeralPort;
  }

  @Override
  public void execute(RatpackServerSpec server) throws Exception {
    RatpackDslClosures closures = new RatpackDslClosures(null);
    RatpackDslBacking backing = new RatpackDslBacking(closures);
    ClosureUtil.configureDelegateFirst(backing, closure);
    server.registry(Guice.registry(ClosureUtil.delegatingAction(closures.getBindings())));
    server.handlers(Groovy.chainAction(closures.getHandlers()));
    ServerConfigBuilder builder = ServerConfig.builder().development(true);
    if (ephemeralPort) {
      builder.port(0);
    }
    ClosureUtil.configureDelegateFirst(builder, closures.getServerConfig());
    server.serverConfig(builder);
  }
}
