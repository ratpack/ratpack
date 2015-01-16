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

package ratpack.server;

import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.server.internal.NettyRatpackServer;

/**
 * The entry point for creating and starting a Ratpack application.
 * <p>
 * The {@link #of(Function)} static method is used to create a server.
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 *
 * import ratpack.test.ApplicationUnderTest;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     RatpackServer server = RatpackServer.of(b -> b
 *       .config(ServerConfig.embedded())            // base server configuration (e.g. port) - optional
 *       .registryOf(r -> r.add(String.class, "foo"))  // registry of supporting objects - optional
 *       .handlers(chain -> chain                    // request handlers - required
 *         .get("a", ctx -> ctx.render(ctx.get(String.class) + " 1"))
 *         .get("b", ctx -> ctx.render(ctx.get(String.class) + " 2"))
 *       )
 *     );
 *
 *     // this method starts the server, via server.start() and then calls server.stop()
 *     ApplicationUnderTest.of(server).test(httpClient -> {
 *       assertEquals("foo 1", httpClient.getText("a"));
 *       assertEquals("foo 2", httpClient.getText("b"));
 *     });
 *   }
 * }
 * }</pre>
 *
 * <h3><a name="def-server-config">Server Config</a></h3>
 * <p>TODO</p>
 * <h3><a name="def-registries">User and server registries</a></h3>
 * <p>TODO</p>
 * <h3><a name="def-handlers">Handlers</a></h3>
 * <p>TODO</p>
 * <p>
 * Server objects are thread safe in that every instance method is {@code synchronized}.
 */
public interface RatpackServer {

  // TODO document the contents of the server registry, and the way that the user registry is joined on to it

  /**
   * Creates a new, unstarted, Ratpack server from the given function.
   * <p>
   * The function argument effectively serves as the definition of the server.
   * It receives a definition builder that it uses to create a definition object.
   * The function is retained internally by the server, and invoked again if the {@link #reload()} method is called.
   *
   * @param definition a function that defines the server
   * @return a Ratpack server
   * @throws Exception any thrown by the given function
   */
  public static RatpackServer of(Function<? super Definition.Builder, ? extends Definition> definition) throws Exception {
    // TODO add more docs above to explain what a user registry actually is

    return new NettyRatpackServer(definition);
  }

  /**
   * Convenience method to {@link #of(Function) define} and {@link #start()} the server in one go.
   *
   * @param serverDefinition a function defines the server, by using the given definition builder
   * @throws Exception any thrown by {@link #of(Function)} or {@link #start()}
   */
  public static void start(Function<? super Definition.Builder, ? extends Definition> serverDefinition) throws Exception {
    of(serverDefinition).start();
  }

  /**
   * The base configuration of this server.
   * <p>
   * {@link #reload() Reloading the server} will generate a new configuration object, causing a different instance to be returned by this method.
   *
   * @return the server configuration
   */
  ServerConfig getServerConfig();

  /**
   * The URL scheme the server uses.
   *
   * @return either <em>http</em> or <em>https</em> depending on whether the server is using SSL or not
   */
  String getScheme();

  /**
   * The actual port that the application is bound to.
   *
   * @return the actual port that the application is bound to, or -1 if the server is not running
   */
  int getBindPort();

  /**
   * The actual host/ip that the application is bound to.
   *
   * @return the actual host/ip that the application is bound to, or null if this server is not running
   */
  @Nullable
  String getBindHost();

  /**
   * Returns {@code true} if the server is running.
   *
   * @return {@code true} if the server is running
   */
  boolean isRunning();

  /**
   * Starts the server, returning as soon as the server is up and ready to receive requests.
   * <p>
   * This will create new threads that are not daemonized.
   * That is, a running Ratpack server will prevent the JVM from shutting down unless {@link System#exit(int)} is called.
   * It is generally advisable to {@link #stop() stop the server} instead of forcefully killing the JVM.
   * <p>
   * Calling this method while the server {@link #isRunning() is running} has no effect.
   *
   * @throws Exception if the server could not be started
   */
  void start() throws Exception;

  /**
   * Stops the server, returning as soon as the server has stopped receiving requests.
   * <p>
   * This method will terminate all threads started by the server.
   *
   * @throws Exception if the server could not be stopped cleanly
   */
  void stop() throws Exception;

  /**
   * Reloads the server from its definition function.
   * <p>
   * The server definition will be rebuilt by executing the function used to create the server.
   * Depending on the function's implementation, a different definition may result.
   * This is effectively configuration reloading mechanism.
   * <pre>{@code
   * import ratpack.server.RatpackServer;
   * import ratpack.test.ApplicationUnderTest;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     String[] holder = new String[]{"foo"};
   *
   *     RatpackServer server = RatpackServer.of(b -> b
   *       .config(ServerConfig.embedded())
   *       .registry(r -> r.add(String.class, holder[0]))
   *       .handler(registry -> (ctx) -> ctx.render(ctx.get(String.class)))
   *     );
   *
   *     ApplicationUnderTest.of(server).test(httpClient -> {
   *       assertEquals("foo", httpClient.getText());
   *
   *       // change data read in definition function…
   *       holder[0] = "bar";
   *
   *       // value unchanged…
   *       assertEquals("foo", httpClient.getText());
   *
   *       // reload server…
   *       server.reload();
   *
   *       // value has changed…
   *       assertEquals("bar", httpClient.getText());
   *     });
   *   }
   * }
   * }</pre>
   * <p>
   * If the server is running, it will be {@link #stop() stopped} and then {@link #start() started}.
   * If it is not running, the definition function will still be executed straight away and the new definition used next time the server is started.
   *
   * @return this
   * @throws Exception any thrown from the definition function, {@link #stop()} or {@link #start()} methods.
   */
  RatpackServer reload() throws Exception;

  /**
   * The internal definition of a Ratpack server.
   * <p>
   * See {@link #of(Function)}.
   */
  interface Definition {

    /**
     * The base server configuration.
     *
     * @return the base server configuration.
     * @see Builder#config(ServerConfig)
     */
    ServerConfig getServerConfig();

    /**
     * The user registry.
     *
     * @return the user registry
     * @see Builder#registry(Registry)
     */
    Function<? super Registry, ? extends Registry> getUserRegistryFactory();

    /**
     * The handler factory.
     *
     * @return the handler factory
     * @see Builder#handler
     */
    Function<? super Registry, ? extends Handler> getHandlerFactory();

    /**
     * A builder for a Ratpack server definition.
     * <p>
     * This builder is used by the function given to the {@link #of(Function)} method used to define a server.
     * <p>
     * The {@link #handler(Function)} or {@link #handlers(Action)} method “terminates” the build and returns the built definition.
     * Calling one of these methods is effectively mandatory, while all other methods are optional.
     * <p>
     * See the documentation of the {@link #of(Function)} method for more detail on how to use this builder.
     */
    interface Builder {

      /**
       * Specifies the user registry.
       * <p>
       * Builds a registry from the given spec, and delegates to {@link #registry(ratpack.registry.Registry)}.
       *
       * @param action the definition of the user registry
       * @return {@code this}
       * @throws Exception any thrown by {@code action}
       * @see #registry(Registry)
       */
      Builder registryOf(Action<? super RegistrySpec> action) throws Exception;

      /**
       * Specifies the user registry.
       * <p>
       * This method is not additive.
       * That is, there is only one user registry and subsequent calls to this method override any previous.
       *
       * @param registry the user registry
       * @return {@code this}
       */
      Builder registry(Registry registry);

      /**
       * Specifies the user registry.
       * <p>
       * This method is not additive.
       * That is, there is only one user registry and subsequent calls to this method override any previous.
       * <p>
       * The given function receives the “base” registry, and must return the “user” registry.
       *
       * @param function a factory for the user registry
       * @return {@code this}
       */
      Builder registry(Function<? super Registry, ? extends Registry> function);

      /**
       * Specifies the server configuration for the application.
       * <p>
       * Server configs can be created by static methods on the {@link ServerConfig} interface, such as {@link ServerConfig#baseDir(java.nio.file.Path)}.
       * <p>
       * The server config returned by {@link ServerConfig#noBaseDir()} is used if this method is not called.
       * <p>
       * This method is not additive.
       * That is, there is only one server config and subsequent calls to this method override any previous.
       *
       * @param serverConfig the server configuration
       * @return {@code this}
       */
      Builder config(ServerConfig serverConfig);

      /**
       * Convenience function that {@link ratpack.server.ServerConfig.Builder#build() builds} the config from the given builder and delegates to {@link #config(ServerConfig)}.
       *
       * @param serverConfigBuilder the server configuration (as a builder)
       * @return {@code this}
       */
      default Builder config(ServerConfig.Builder serverConfigBuilder) {
        return config(serverConfigBuilder.build());
      }

      /**
       * Builds the server definition from the given handler type, and state of this builder.
       * <p>
       * The handler is retrieved from the registry.
       *
       * @param handlerType the type of handler to retrieve from the registry
       * @return a server definition based on the state of this builder
       */
      default Definition handler(Class<? extends Handler> handlerType) {
        return handler(registry -> registry.get(handlerType));
      }

      /**
       * Builds the server definition from the given factory, and state of this builder.
       * <p>
       * The registry given to this method is not the same registry that is defined by the {@link #registry(Registry)} methods (i.e. the user registry).
       * It also contains additional entries added to all Ratpack applications.
       * <p>
       * The {@link Handlers} type provides handler implementations that may be of use.
       *
       * @param handlerFactory a factory function for the root handler
       * @return a server definition based on the state of this builder
       * @see Handlers
       * @see #handlers(Action)
       */
      Definition handler(Function<? super Registry, ? extends Handler> handlerFactory);

      /**
       * Builds the server definition from the given handler chain definition, and state of this builder.
       * <p>
       * The server registry is available during the action via the {@link Chain#getRegistry()} method of the given chain.
       *
       * @param handlers a handler defining action
       * @return a server definition based on the state of this builder
       * @see Chain
       */
      default Definition handlers(Action<? super Chain> handlers) {
        return handler(r -> Handlers.chain(r, handlers));
      }
    }

  }

}
