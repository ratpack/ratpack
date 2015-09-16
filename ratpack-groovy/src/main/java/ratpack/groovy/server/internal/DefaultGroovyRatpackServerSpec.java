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

package ratpack.groovy.server.internal;

import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.groovy.server.GroovyRatpackServerSpec;
import ratpack.handling.Handler;
import ratpack.registry.Registry;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;

public class DefaultGroovyRatpackServerSpec implements GroovyRatpackServerSpec {

  private final RatpackServerSpec delegate;

  public DefaultGroovyRatpackServerSpec(RatpackServerSpec delegate) {
    this.delegate = delegate;
  }

  @Override
  public GroovyRatpackServerSpec registry(Function<? super Registry, ? extends Registry> function) {
    delegate.registry(function);
    return this;
  }

  @Override
  public GroovyRatpackServerSpec serverConfig(ServerConfig serverConfig) {
    delegate.serverConfig(serverConfig);
    return this;
  }

  @Override
  public GroovyRatpackServerSpec serverConfig(Action<? super ServerConfigBuilder> action) throws Exception {
    delegate.serverConfig(action);
    return this;
  }

  @Override
  public GroovyRatpackServerSpec handler(Function<? super Registry, ? extends Handler> handlerFactory) {
    delegate.handler(handlerFactory);
    return this;
  }
}
