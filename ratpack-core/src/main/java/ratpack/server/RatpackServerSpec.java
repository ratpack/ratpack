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

package ratpack.server;

import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

/**
 * A buildable specification of a Ratpack server.
 * <p>
 * This type is used when creating a new Ratpack server, via {@link RatpackServer#of(Action)}.
 * <p>
 * There are three aspects of a Ratpack server, which can be set via the following methods:
 * <ul>
 * <li>{@link #serverConfig(ServerConfig)} - the initial information needed to start the server</li>
 * <li>{@link #registry(Function)} - the registry of objects making up the application</li>
 * <li>{@link #handler(Function)} - the request handling chain</li>
 * </ul>
 * <p>
 * The other methods of this interface are alternate, sometimes more convenient or concise, variants of those methods.
 * <p>
 * None of the methods of this interface are additive (i.e. calling {@link #handlers(Action)} twice will result in the second “value” being used).
 * <p>
 * All of the methods are effectively optional, as there are default values for the three different aspects (each of the base methods details what the default value is).
 * However, in practice you almost always want to at least set a request handler.
 */
public interface RatpackServerSpec {

  /**
   * Builds the user registry via the given spec action.
   *
   * @param action the definition of the user registry
   * @return {@code this}
   * @throws Exception any thrown by {@code action}
   * @see #registry(ratpack.registry.Registry)
   */
  default RatpackServerSpec registryOf(Action<? super RegistrySpec> action) throws Exception {
    return registry(r -> Registry.of(action));
  }

  /**
   * Sets the user registry to exactly the given registry.
   *
   * @param registry the user registry
   * @return {@code this}
   */
  default RatpackServerSpec registry(Registry registry) {
    return registry(r -> registry);
  }

  /**
   * Sets the user registry as the return value of the given function.
   * <p>
   * The given function receives the “base” registry (i.e. the base infrastructure provided by Ratpack) as its argument.
   * <p>
   * If a user registry is not set, an {@link Registry#empty() empty registry} will be used.
   *
   * @param function a function that provides the user registry
   * @return {@code this}
   */
  RatpackServerSpec registry(Function<? super Registry, ? extends Registry> function);

  /**
   * Convenience function that {@link ServerConfigBuilder#build() builds} the config from the given builder and delegates to {@link #serverConfig(ServerConfig)}.
   *
   * @param builder the server configuration (as a builder)
   * @return {@code this}
   */
  default RatpackServerSpec serverConfig(ServerConfigBuilder builder) {
    return serverConfig(builder.build());
  }

  /**
   * Sets the server configuration for the application.
   * <p>
   * Server configs can be created via {@link ServerConfig#builder()}.
   *
   * @param serverConfig the server configuration
   * @return {@code this}
   */
  RatpackServerSpec serverConfig(ServerConfig serverConfig);

  default RatpackServerSpec serverConfig(Action<? super ServerConfigBuilder> action) throws Exception {
    return serverConfig(ServerConfig.of(action));
  }

  /**
   * Sets the root handler by getting a handler of the given type from the server registry.
   * <p>
   * This can be useful when integrating with something that can wire together objects, such as Google Guice.
   * <pre class="java">{@code
   * import ratpack.guice.Guice;
   * import ratpack.handling.Context;
   * import ratpack.handling.Handler;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import javax.inject.Inject;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static class MyHandler implements Handler {
   *     private final String value;
   *
   *     {@literal @}Inject
   *     public MyHandler(String value) {
   *       this.value = value;
   *     }
   *
   *     public void handle(Context ctx) throws Exception {
   *       ctx.render(value);
   *     }
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registry(Guice.registry(b -> b
   *             .bindInstance("Hello World!")
   *             .bind(MyHandler.class)
   *         ))
   *         .handler(MyHandler.class)
   *     ).test(httpClient ->
   *         assertEquals("Hello World!", httpClient.getText())
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param handlerType the type of handler to retrieve from the registry
   * @return {@code this}
   */
  default RatpackServerSpec handler(Class<? extends Handler> handlerType) {
    return handler(registry -> registry.get(handlerType));
  }

  /**
   * Sets the root handler to the chain specified by the given action.
   * <p>
   * The server registry is available during the action via the {@link ratpack.handling.Chain#getRegistry()} method of the given chain.
   *
   * @param handlers an action defining a handler chain
   * @return {@code this}
   * @see Chain
   */
  default RatpackServerSpec handlers(Action<? super Chain> handlers) {
    return handler(r -> Handlers.chain(r, handlers));
  }

  /**
   * Sets the root handler to the return of the given function.
   * <p>
   * The given function receives the effective <em>server</em> registry.
   * This is the <em>base</em> registry (common Ratpack infrastructure) {@link Registry#join(Registry) joined} with the <em>user</em> registry (i.e. the registry set on this spec).
   * <p>
   * All requests will be routed to the given handler.
   * <p>
   * Generally, it is more convenient to use the {@link #handlers(Action)} method than this as it makes it easy to build a handler chain.
   * <p>
   * The {@link Handlers} type provides handler implementations that may be of use.
   * <p>
   * If a handler is not set, the handler returned by {@link Handlers#notFound()} will be used (i.e. all requests will result in a 404).
   *
   * @param handlerFactory a factory for the root handler
   * @return {@code this}
   * @see Handlers
   * @see #handlers(Action)
   */
  RatpackServerSpec handler(Function<? super Registry, ? extends Handler> handlerFactory);

}

