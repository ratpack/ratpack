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

import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.server.internal.NettyRatpackServer;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * A Ratpack server.
 */
public interface RatpackServer {

  /**
   * Creates a builder of ratpack servers.
   *
   * @param app a function that builds the definition for this server.
   * @return a new, non started Ratpack server.
   */
  public static RatpackServer of(Function<? super Definition.Builder, ? extends Definition> app) {
    return uncheck(() -> app.apply(Definition.builder()).build());
  }

  /**
   * Convenience method to {@link #of(Function)} and {@link #start()} the server in one go.
   *
   * @param app a factory function that defines the server
   * @throws Exception any thrown by {@link #start()}
   */
  public static void start(Function<? super Definition.Builder, ? extends Definition> app) throws Exception {
    of(app).start();
  }

  public final class Definition {
    private final ServerConfig serverConfig;
    private final Registry userRegistry;
    private final Function<? super Registry, ? extends Handler> handlerFactory;

    private Definition(ServerConfig serverConfig, Registry userRegistry, Function<? super Registry, ? extends Handler> handlerFactory) {
      this.serverConfig = serverConfig;
      this.userRegistry = userRegistry;
      this.handlerFactory = handlerFactory;
    }

    public static Builder builder() {
      return new Builder();
    }

    public ServerConfig getServerConfig() {
      return serverConfig;
    }

    public Registry getUserRegistry() {
      return userRegistry;
    }

    public Function<? super Registry, ? extends Handler> getHandlerFactory() {
      return handlerFactory;
    }

    public static final class Builder {

      private ServerConfig serverConfig = ServerConfig.noBaseDir().build();
      private Registry userRegistry = Registries.empty();

      private Builder() {
      }

      /**
       * Specifies the “base” registry.
       * <p>
       * Builds a registry from the given spec, and delegates to {@link #registry(Registry)}.
       *
       * @param action the definition of the base registry
       * @return this
       * @throws Exception any thrown by {@code action}
       */
      public Builder registry(Action<? super RegistrySpec> action) throws Exception {
        return registry(Registries.registry(action));
      }

      /**
       * Specifies the “base” registry.
       * <p>
       * This method is not additive.
       * That is, there is only one “base” registry and subsequent calls to this method override the previous.
       *
       * @param registry the base registry
       * @return this
       */
      public Builder registry(Registry registry) {
        this.userRegistry = registry;
        return this;
      }

      /**
       * Specify the server configuration for the application
       *
       * @param serverConfig the server configuration
       * @return this
       */
      public Builder config(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        return this;
      }

      /**
       * Builds a server definition from the given factory, supplying the {@link Registry#join(Registry) joining} of the root and base registries (if one was specified).
       *
       * @param handlerFactory a factory function for the root handler
       * @return a definition for the Ratpack server
       */
      public Definition build(Function<? super Registry, ? extends Handler> handlerFactory) {
        return new Definition(serverConfig, userRegistry, handlerFactory);
      }
    }

    /**
     * Constructs a RatpackServer instance from this definition.
     *
     * @return a new, not yet started Ratpack server
     */
    public RatpackServer build() {
      return new NettyRatpackServer(this);
    }

  }

  /**
   * The (read only) configuration that was used to launch this server.
   *
   * @return the (read only) configuration that was used to launch this server.
   */
  ServerConfig getServerConfig();

  /**
   * The URL scheme the server uses.
   *
   * @return Either <em>http</em> or <em>https</em> depending on whether the server is using SSL or not.
   */
  String getScheme();

  /**
   * The actual port that the application is bound to.
   *
   * @return The actual port that the application is bound to, or -1 if the server is not running.
   */
  int getBindPort();

  /**
   * The actual host/ip that the application is bound to.
   *
   * @return The actual host/ip that the application is bound to, or null if this server is not running.
   */
  String getBindHost();

  /**
   * Returns {@code true} if the server is running.
   *
   * @return {@code true} if the server is running.
   */
  boolean isRunning();

  /**
   * Starts the server, returning as soon as the server is up and ready to receive requests.
   * <p>
   * This will create new threads that are not daemonized.
   *
   * @throws Exception if the server could not be started
   */
  void start() throws Exception;

  /**
   * Stops the server, returning as soon as the server has stopped receiving requests.
   *
   * @throws Exception if the server could not be stopped cleanly
   */
  void stop() throws Exception;

  /**
   * Reloads the server from its definition.
   *
   * @return a new Ratpack server in the same state as the current server.
   *
   * @throws Exception any exceptions from constructing or starting the server
   */
  RatpackServer reload() throws Exception;

}
