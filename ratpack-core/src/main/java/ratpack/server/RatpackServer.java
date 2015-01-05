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

import com.google.common.base.Throwables;
import ratpack.file.BaseDirRequiredException;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.internal.FactoryHandler;
import ratpack.launch.LaunchException;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.server.internal.NettyRatpackServer;
import ratpack.server.internal.RatpackChannelInitializer;

import java.io.File;

/**
 * A Ratpack server.
 */
public interface RatpackServer {

  /**
   * Create a server builder with the given server config.
   *
   * @param config the server configuration
   * @return a server builder
   */
  public static Builder with(ServerConfig config) {
    return new Builder(config);
  }

  /**
   * Create a server builder with a default {@link ServerConfig}.
   *
   * @return a server builder
   */
  public static Builder withDefaults() {
    return with(ServerConfig.noBaseDir().build());
  }

  /**
   * A builder for a Ratpack server.
   */
  final class Builder {
    private final ServerConfig serverConfig;
    private Registry userRegistry = Registries.empty();

    private Builder(ServerConfig serverConfig) {
      this.serverConfig = serverConfig;
    }

    /**
     * Builds a server from the given factory, supplying the {@link Registry#join(Registry) joining} of the root and base registries (if one was specified).
     *
     * @param handlerFactory a factory function for the root handler
     * @return a new, not yet started, Ratpack server
     */
    public RatpackServer build(Function<? super Registry, ? extends Handler> handlerFactory) {
      Registry baseRegistry = NettyRatpackServer.baseRegistry(serverConfig, userRegistry);

      return new NettyRatpackServer(baseRegistry, stopper -> {
        Handler handler = null;

        if (serverConfig.isDevelopment()) {
          File classFile = ClassUtil.getClassFile(handlerFactory);
          if (classFile != null) {
            Factory<Handler> factory = new ReloadableFileBackedFactory<>(classFile.toPath(), true, (file, bytes) -> createHandler(baseRegistry, handlerFactory));
            handler = new FactoryHandler(factory);
          }
        }

        if (handler == null) {
          handler = createHandler(baseRegistry, handlerFactory);
        }

        return new RatpackChannelInitializer(baseRegistry, handler, stopper);
      });
    }

    private static Handler createHandler(Registry rootRegistry, Function<? super Registry, ? extends Handler> handlerFactory) {
      try {
        return handlerFactory.apply(rootRegistry);
      } catch (Exception e) {
        Throwables.propagateIfInstanceOf(e, BaseDirRequiredException.class);
        throw new LaunchException("Could not create handler via handler factory: " + handlerFactory.getClass().getName(), e);
      }
    }

    /**
     * Convenience method to {@link #build(Function)} and {@link #start()} the server in one go.
     *
     * @param handlerFactory a factory function for the root handler
     * @throws Exception any thrown by {@link #start()}
     */
    public void start(Function<? super Registry, ? extends Handler> handlerFactory) throws Exception {
      build(handlerFactory).start();
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
