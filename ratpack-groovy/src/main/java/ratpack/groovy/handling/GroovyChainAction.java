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

package ratpack.groovy.handling;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.groovy.handling.internal.DefaultGroovyChain;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

/**
 * Convenient super class for packaging up Groovy handler chain logic.
 * <p>
 * Implementations can naturally use the {@link GroovyChain} DSL in their implementation of {@link #execute()}.
 * <pre class="tested">
 * import ratpack.groovy.handling.GroovyChainAction
 * import static ratpack.groovy.test.TestHttpClients.testHttpClient
 * import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp
 *
 * def app = embeddedApp {
 *   handlers {
 *     get("someHandler") {
 *       render "someHandler"
 *     }
 *
 *     // Include the handlers defined in OtherHandlers
 *     handler chain(new OtherHandlers())
 *   }
 * }
 *
 * // In another file…
 *
 * class OtherHandlers extends GroovyChainAction {
 *   protected void execute() {
 *     // The GroovyChain DSL can be used in this method
 *
 *     get("foo") {
 *       render "foo"
 *     }
 *     get("bar") {
 *       render "bar"
 *     }
 *   }
 * }
 *
 * // Functionally test the whole app…
 *
 * def client = testHttpClient(app)
 *
 * assert client.getText("someHandler") == "someHandler"
 * assert client.getText("foo") == "foo"
 * assert client.getText("bar") == "bar"
 *
 * app.close()
 *
 * // Factoring out into GroovyChainAction implementations mean they can be unit tested in isolation…
 *
 * import ratpack.handling.Handlers
 * import static ratpack.groovy.test.GroovyUnitTest.handle
 *
 * assert handle(new OtherHandlers()) { uri "bar" }.rendered(String) == "bar"
 * assert handle(new OtherHandlers()) { uri "foo" }.rendered(String) == "foo"
 * </pre>
 * <p>
 * This class implements the {@link GroovyChain} interface by delegating each method to the chain returned by {@link #getChain()}.
 * This method only returns a value during execution of {@link Action#execute(Object)}, which is the given chain available as a {@link GroovyChain}.
 */
public abstract class GroovyChainAction implements Action<Chain>, GroovyChain {

  private GroovyChain chain;

  @Override
  public GroovyChain assets(String path, String... indexFiles) {
    return getChain().assets(path, indexFiles);
  }

  @Override
  public Handler chain(Action<? super Chain> action) throws Exception {
    return getChain().chain(action);
  }

  @Override
  public GroovyChain delete(Handler handler) {
    return getChain().delete(handler);
  }

  @Override
  public GroovyChain delete(String path, Handler handler) {
    return getChain().delete(path, handler);
  }

  @Override
  public GroovyChain delete(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().delete(path, handler);
  }

  @Override
  public GroovyChain delete(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().delete(handler);
  }

  /**
   * Delegates to {@link #execute()}, using the given {@code chain} for delegation.
   *
   * @param chain The chain to add handlers to
   * @throws Exception Any thrown by {@link #execute()}
   */
  @Override
  public final void execute(Chain chain) throws Exception {
    try {
      this.chain = new DefaultGroovyChain(chain);
      execute();
    } finally {
      this.chain = null;
    }
  }

  /**
   * Implementations can naturally use the {@link GroovyChain} DSL for the duration of this method.
   * <p>
   * See the {@link GroovyChainAction class level documentation} for an implementation example.
   *
   * @throws Exception Any exception thrown while defining the handlers
   */
  abstract protected void execute() throws Exception;

  @Override
  public GroovyChain fileSystem(String path, Handler handler) {
    return getChain().fileSystem(path, handler);
  }

  @Override
  public GroovyChain fileSystem(String path, Action<? super Chain> action) throws Exception {
    return getChain().fileSystem(path, action);
  }

  @Override
  public GroovyChain fileSystem(String path, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) throws Exception {
    return getChain().fileSystem(path, handlers);
  }

  @Override
  public GroovyChain get(Handler handler) {
    return getChain().get(handler);
  }

  @Override
  public GroovyChain get(String path, Handler handler) {
    return getChain().get(path, handler);
  }

  @Override
  public GroovyChain get(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().get(path, handler);
  }

  @Override
  public GroovyChain get(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().get(handler);
  }

  protected GroovyChain getChain() throws IllegalStateException {
    if (chain == null) {
      throw new IllegalStateException("no chain set - GroovyChain methods should only be called during execute()");
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
  public GroovyChain handler(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().handler(handler);
  }

  @Override
  public GroovyChain handler(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().handler(path, handler);
  }

  @Override
  public GroovyChain handler(Handler handler) {
    return getChain().handler(handler);
  }

  @Override
  public GroovyChain handler(String path, Handler handler) {
    return getChain().handler(path, handler);
  }

  @Override
  public GroovyChain header(String headerName, String headerValue, Handler handler) {
    return getChain().header(headerName, headerValue, handler);
  }

  @Override
  public GroovyChain header(String headerName, String headerValue, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().header(headerName, headerValue, handler);
  }

  @Override
  public GroovyChain host(String hostName, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return getChain().host(hostName, closure);
  }

  @Override
  public GroovyChain host(String hostName, Action<? super Chain> action) throws Exception {
    return getChain().host(hostName, action);
  }

  @Override
  public GroovyChain patch(Handler handler) {
    return getChain().patch(handler);
  }

  @Override
  public GroovyChain patch(String path, Handler handler) {
    return getChain().patch(path, handler);
  }

  @Override
  public GroovyChain patch(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().patch(path, handler);
  }

  @Override
  public GroovyChain patch(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().patch(handler);
  }

  @Override
  public GroovyChain post(String path, Handler handler) {
    return getChain().post(path, handler);
  }

  @Override
  public GroovyChain post(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().post(path, handler);
  }

  @Override
  public GroovyChain post(Handler handler) {
    return getChain().post(handler);
  }

  @Override
  public GroovyChain post(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().post(handler);
  }

  @Override
  public GroovyChain prefix(String prefix, Handler handler) {
    return getChain().prefix(prefix, handler);
  }

  @Override
  public GroovyChain prefix(String prefix, Action<? super Chain> action) throws Exception {
    return getChain().prefix(prefix, action);
  }

  @Override
  public GroovyChain prefix(String prefix, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> chain) throws Exception {
    return getChain().prefix(prefix, chain);
  }

  @Override
  public GroovyChain put(Handler handler) {
    return getChain().put(handler);
  }

  @Override
  public GroovyChain put(String path, Handler handler) {
    return getChain().put(path, handler);
  }

  @Override
  public GroovyChain put(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().put(path, handler);
  }

  @Override
  public GroovyChain put(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return getChain().put(handler);
  }

  @Override
  public GroovyChain register(Registry registry) {
    return chain.register(registry);
  }

  @Override
  public GroovyChain register(Action<? super RegistrySpec> action) throws Exception {
    return chain.register(action);
  }

  @Override
  public GroovyChain register(Registry registry, Handler handler) {
    return getChain().register(registry, handler);
  }

  @Override
  public GroovyChain register(Registry registry, Action<? super Chain> action) throws Exception {
    return getChain().register(registry, action);
  }

  @Override
  public GroovyChain register(Action<? super RegistrySpec> registryAction, Handler handler) throws Exception {
    return getChain().register(registryAction, handler);
  }

  @Override
  public GroovyChain register(Action<? super RegistrySpec> registryAction, Action<? super Chain> chainAction) throws Exception {
    return getChain().register(registryAction, chainAction);
  }

  @Override
  public GroovyChain register(Registry registry, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) throws Exception {
    return getChain().register(registry, handlers);
  }

  @Override
  public GroovyChain register(@DelegatesTo(value = RegistrySpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return getChain().register(closure);
  }

  @Override
  public GroovyChain register(Action<? super RegistrySpec> registryAction, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) throws Exception {
    return getChain().register(registryAction, handler);
  }

  @Override
  public GroovyChain insert(Action<? super Chain> action) throws Exception {
    return getChain().insert(action);
  }

  @Override
  public Handler chain(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return getChain().chain(closure);
  }

  @Override
  public GroovyChain insert(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return getChain().insert(closure);
  }

  @Override
  public GroovyChain redirect(int code, String location) { return getChain().redirect(code, location); }

}
