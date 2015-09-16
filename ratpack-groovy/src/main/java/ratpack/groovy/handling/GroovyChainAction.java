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

package ratpack.groovy.handling;

import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;

/**
 * A convenience super class for a standalone implementation of a {@code Action<GroovyChain>}.
 * <p>
 * Subclasses implement the {@link #execute()} method, and implicitly program against the {@link GroovyChain} DSL.
 *
 * <pre class="tested-dynamic">{@code
 * import ratpack.groovy.handling.GroovyChainAction
 * import ratpack.groovy.test.embed.GroovyEmbeddedApp
 *
 * class Handlers extends GroovyChainAction {
 *   public void execute() throws Exception {
 *     path { render "root" }
 *     prefix("foo") {
 *       path("bar") { render "foobar" }
 *     }
 *   }
 * }
 *
 * GroovyEmbeddedApp.of {
 *   handlers new Handlers()
 * } test {
 *   assert getText() == "root"
 *   assert getText("foo/bar") == "foobar"
 * }
 * }</pre>
 */
public abstract class GroovyChainAction implements GroovyChain, Action<Chain> {

  private final ThreadLocal<GroovyChain> delegate = new ThreadLocal<>();

  /**
   * Defines the handler chain.
   *
   * @throws Exception any
   */
  public abstract void execute() throws Exception;

  /**
   * Invokes {@link #execute()} while setting the given chain as the implicit receiver.
   *
   * @param chain the handler chain
   * @throws Exception any
   */
  @Override
  public final void execute(Chain chain) throws Exception {
    try {
      delegate.set(GroovyChain.from(chain));
      execute();
    } finally {
      delegate.remove();
    }
  }

  private GroovyChain getDelegate() {
    GroovyChain delegate = this.delegate.get();
    if (delegate == null) {
      throw new IllegalStateException("delegate requested outside of execute");
    }
    return delegate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GroovyChain all(Handler handler) {
    return getDelegate().all(handler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServerConfig getServerConfig() {
    return getDelegate().getServerConfig();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Registry getRegistry() throws IllegalStateException {
    return getDelegate().getRegistry();
  }

}
