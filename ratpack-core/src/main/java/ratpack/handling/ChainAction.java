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

package ratpack.handling;

import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

/**
 * Convenient super class for packaging up Groovy handler chain logic.
 * <p>
 * Implementations can naturally use the {@link Chain} DSL in their implementation of {@link #execute()}.
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.handling.ChainAction;
 *
 * public class MyHandlers extends ChainAction {
 *   protected void execute() {
 *     get("foo", new Handler() {
 *       public void handle(Context context) {
 *          context.render("foo")
 *       }
 *     });
 *     get("bar", new Handler() {
 *       public void handle(Context context) {
 *          context.render("bar")
 *       }
 *     });
 *   }
 * }
 *
 * // Tests (Groovy) …
 *
 * import ratpack.test.embed.PathBaseDirBuilder
 * import ratpack.groovy.test.TestHttpClients
 * import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp
 *
 * def app = embeddedApp {
 *   handlers {
 *     handler chain(new MyHandlers())
 *   }
 * }
 *
 * def client = TestHttpClients.testHttpClient(app)
 *
 * assert client.getText("foo") == "foo"
 * assert client.getText("bar") == "bar"
 *
 * app.close()
 *
 * // Factoring out into ChainAction implementations means they can be unit tested in isolation…
 *
 * import ratpack.handling.Handlers;
 * import ratpack.test.MockLaunchConfig;
 * import ratpack.launch.LaunchConfig;
 * import ratpack.test.handling.Invocation;
 * import ratpack.test.handling.InvocationBuilder;
 * import ratpack.func.Action;
 *
 * import static ratpack.test.UnitTest.invoke;
 *
 * LaunchConfig launchConfig = new MockLaunchConfig();
 * Handler handler = Handlers.chain(launchConfig, new MyHandlers());
 *
 * Invocation invocation = invoke(handler, new Action&lt;InvocationBuilder&gt;() {
 *   public void execute(InvocationBuilder builder) {
 *     builder.uri("foo");
 *   }
 * });
 * assert invocation.rendered(String.class).equals("foo");
 *
 * invocation = invoke(handler, new Action&lt;InvocationBuilder&gt;() {
 *   public void execute(InvocationBuilder builder) {
 *     builder.uri("bar");
 *   }
 * });
 * assert invocation.rendered(String.class).equals("bar");
 * </pre>
 * <p>
 * This class implements the {@link Chain} interface by delegating each method to the chain returned by {@link #getChain()}.
 * This method only returns a value during execution of {@link #execute(Chain)}, which is the given chain.
 */
public abstract class ChainAction implements Action<Chain>, Chain {

  private Chain chain;

  @Override
  public Chain assets(String path, String... indexFiles) {
    return getChain().assets(path, indexFiles);
  }

  @Override
  public Handler chain(Action<? super Chain> action) throws Exception {
    return getChain().chain(action);
  }

  @Override
  public Chain delete(String path, Handler handler) {
    return getChain().delete(path, handler);
  }

  @Override
  public Chain delete(Handler handler) {
    return getChain().delete(handler);
  }

  /**
   * Delegates to {@link #execute()}, using the given {@code chain} for delegation.
   *
   * @param chain The chain to add handlers to
   * @throws Exception Any thrown by {@link #execute()}
   */
  public final void execute(Chain chain) throws Exception {
    try {
      this.chain = chain;
      execute();
    } finally {
      this.chain = null;
    }
  }

  /**
   * Implementations can naturally use the {@link Chain} DSL for the duration of this method.
   * <p>
   * See the {@link ChainAction class level documentation} for an implementation example.
   *
   * @throws Exception Any exception thrown while defining the handlers
   */
  protected abstract void execute() throws Exception;

  @Override
  public Chain fileSystem(String path, Handler handler) {
    return getChain().fileSystem(path, handler);
  }

  @Override
  public Chain fileSystem(String path, Action<? super Chain> action) throws Exception {
    return getChain().fileSystem(path, action);
  }

  @Override
  public Chain get(String path, Handler handler) {
    return getChain().get(path, handler);
  }

  @Override
  public Chain get(Handler handler) {
    return getChain().get(handler);
  }

  protected Chain getChain() throws IllegalStateException {
    if (chain == null) {
      throw new IllegalStateException("no chain set - Chain methods should only be called during execute()");
    }
    return chain;
  }

  @Override
  public LaunchConfig getLaunchConfig() {
    return getChain().getLaunchConfig();
  }

  @Override
  @Nullable
  public Registry getRegistry() {
    return getChain().getRegistry();
  }

  @Override
  public Chain handler(Handler handler) {
    return getChain().handler(handler);
  }

  @Override
  public Chain handler(String path, Handler handler) {
    return getChain().handler(path, handler);
  }

  @Override
  public Chain header(String headerName, String headerValue, Handler handler) {
    return getChain().header(headerName, headerValue, handler);
  }

  @Override
  public Chain patch(String path, Handler handler) {
    return getChain().patch(path, handler);
  }

  @Override
  public Chain patch(Handler handler) {
    return getChain().patch(handler);
  }

  @Override
  public Chain post(String path, Handler handler) {
    return getChain().post(path, handler);
  }

  @Override
  public Chain post(Handler handler) {
    return getChain().post(handler);
  }

  @Override
  public Chain prefix(String prefix, Handler handler) {
    return getChain().prefix(prefix, handler);
  }

  @Override
  public Chain prefix(String prefix, Action<? super Chain> action) throws Exception {
    return getChain().prefix(prefix, action);
  }

  @Override
  public Chain put(String path, Handler handler) {
    return getChain().put(path, handler);
  }

  @Override
  public Chain put(Handler handler) {
    return getChain().put(handler);
  }

  @Override
  public Chain register(Registry registry, Handler handler) {
    return getChain().register(registry, handler);
  }

  @Override
  public Chain register(Registry registry, Action<? super Chain> action) throws Exception {
    return getChain().register(registry, action);
  }

  @Override
  public Chain register(Action<? super RegistrySpec> registryAction, Handler handler) throws Exception {
    return getChain().register(registryAction, handler);
  }

  @Override
  public Chain register(Action<? super RegistrySpec> registryAction, Action<? super Chain> action) throws Exception {
    return getChain().register(registryAction, action);
  }

  @Override
  public Chain register(Registry registry) {
    return getChain().register(registry);
  }

  @Override
  public Chain register(Action<? super RegistrySpec> action) throws Exception {
    return getChain().register(action);
  }

}
