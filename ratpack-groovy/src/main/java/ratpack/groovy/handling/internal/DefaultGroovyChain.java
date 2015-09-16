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

import ratpack.groovy.handling.GroovyChain;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;

public class DefaultGroovyChain implements GroovyChain {

  private final Chain delegate;

  public DefaultGroovyChain(Chain delegate) {
    this.delegate = delegate;
  }

  @Override
  public GroovyChain all(Handler handler) {
    delegate.all(handler);
    return this;
  }

  @Override
  public ServerConfig getServerConfig() {
    return delegate.getServerConfig();
  }

  @Override
  public Registry getRegistry() throws IllegalStateException {
    return delegate.getRegistry();
  }

}
