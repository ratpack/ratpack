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

package ratpack.groovy.server;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.groovy.Groovy;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.server.internal.DefaultGroovyRatpackServerSpec;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;

public interface GroovyRatpackServerSpec extends RatpackServerSpec {

  static GroovyRatpackServerSpec from(RatpackServerSpec spec) {
    return spec instanceof GroovyRatpackServerSpec ? (GroovyRatpackServerSpec) spec : new DefaultGroovyRatpackServerSpec(spec);
  }

  default GroovyRatpackServerSpec handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return handler(r -> Handlers.chain(r, Groovy.chainAction(handlers)));
  }

  @Override
  GroovyRatpackServerSpec handler(Function<? super Registry, ? extends Handler> handlerFactory);

  @Override
  GroovyRatpackServerSpec registry(Function<? super Registry, ? extends Registry> function);

  @Override
  GroovyRatpackServerSpec serverConfig(ServerConfig serverConfig);

  @Override
  GroovyRatpackServerSpec serverConfig(Action<? super ServerConfigBuilder> action) throws Exception;

  default GroovyRatpackServerSpec serverConfig(@DelegatesTo(value = ServerConfigBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> action) throws Exception {
    return from(RatpackServerSpec.super.serverConfig(ClosureUtil.delegatingAction(ServerConfigBuilder.class, action)));
  }

  default GroovyRatpackServerSpec registryOf(@DelegatesTo(value = RegistrySpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> action) throws Exception {
    return from(RatpackServerSpec.super.registryOf(ClosureUtil.delegatingAction(RegistrySpec.class, action)));
  }
}
