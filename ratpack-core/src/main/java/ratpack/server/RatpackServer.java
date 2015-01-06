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
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.internal.FactoryHandler;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.server.internal.BaseRegistry;
import ratpack.server.internal.NettyRatpackServer;
import ratpack.server.internal.RatpackChannelInitializer;

import java.io.File;

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
    return uncheck(() -> app.apply(new Definition().builder()).build());
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

  final class Definition {

    private ServerConfig serverConfig = ServerConfig.noBaseDir().build();
    private Registry userRegistry = Registries.empty();
    private Function<? super Registry, ? extends Handler> handlerFactory;

    //TODO is this the correct signature? I couldn't access the method on Builder without adding public here.
    public final class Builder {

      private final Definition definition;

      private Builder(Definition definition) {
        this.definition = definition;
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
        this.definition.userRegistry = registry;
        return this;
      }

      /**
       * Specify the server configuration for the application
       *
       * @param serverConfig the server configuration
       * @return this
       */
      public Builder config(ServerConfig serverConfig) {
        this.definition.serverConfig = serverConfig;
        return this;
      }

      /**
       * Builds a server definition from the given factory, supplying the {@link Registry#join(Registry) joining} of the root and base registries (if one was specified).
       *
       * @param handlerFactory a factory function for the root handler
       * @return a definition for the Ratpack server
       */
      public Definition build(Function<? super Registry, ? extends Handler> handlerFactory) {
        this.definition.handlerFactory = handlerFactory;
        return this.definition;
      }
    }

    public Builder builder() {
      return new Builder(this);
    }

    /**
     * Constructs a RatpackServer instance from this definition.
     *
     * @return a new, not yet started Ratpack server
     */
    public RatpackServer build() {
      Registry baseRegistry = BaseRegistry.baseRegistry(serverConfig, userRegistry);

      return new NettyRatpackServer(this, baseRegistry, stopper -> {
        Handler handler = null;

        if (serverConfig.isDevelopment()) {
          File classFile = ClassUtil.getClassFile(handlerFactory);
          if (classFile != null) {
            Factory<Handler> factory = new ReloadableFileBackedFactory<>(classFile.toPath(), true, (file, bytes) -> handlerFactory.apply(baseRegistry));
            handler = new FactoryHandler(factory);
          }
        }

        if (handler == null) {
          handler = handlerFactory.apply(baseRegistry);
        }

        return new RatpackChannelInitializer(baseRegistry, handler, stopper);
      });
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

}
