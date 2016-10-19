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

package ratpack.test.embed;

import org.slf4j.LoggerFactory;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;
import ratpack.server.internal.EmbeddedRatpackServerSpec;
import ratpack.test.CloseableApplicationUnderTest;
import ratpack.test.embed.internal.EmbeddedAppSupport;

import java.net.URI;

import static ratpack.util.Exceptions.uncheck;

/**
 * An application created and used at runtime, useful for functionally testing subsets of functionality.
 * <p>
 * This mechanism can be used for functionally testing isolated sections of an application,
 * or for testing general libraries that provide reusable functionality (e.g. Ratpack Guice modules).
 * <p>
 * Different implementations expose different API that can be used to define the actual application under test.
 * <p>
 * As embedded applications also implement {@link ratpack.test.ApplicationUnderTest}, they are suitable for use with clients accessing the app via HTTP.
 * Implementations must ensure that the application is up and receiving request when returning from {@link #getAddress()}.
 * Be sure to {@link #close()} the application after use to free resources.
 */
public interface EmbeddedApp extends CloseableApplicationUnderTest {

  /**
   * Creates an embedded application for the given server.
   *
   * @param server the server to embed
   * @return a newly created embedded application
   */
  static EmbeddedApp fromServer(RatpackServer server) {
    return fromServer(() -> server);
  }

  /**
   * Creates an embedded application from the given function.
   *
   * @param definition a function that defines the server
   * @return a newly created embedded application
   * @throws java.lang.Exception if an error is encountered creating the application
   * @see ratpack.server.RatpackServer#of(Action)
   */
  static EmbeddedApp of(Action<? super RatpackServerSpec> definition) throws Exception {
    return fromServer(RatpackServer.of(d -> definition.execute(new EmbeddedRatpackServerSpec(d))));
  }

  /**
   * Creates an embedded application for the given server.
   *
   * @param server a factory that creates the server to embed
   * @return a newly created embedded application
   */
  static EmbeddedApp fromServer(Factory<? extends RatpackServer> server) {
    return new EmbeddedAppSupport() {
      @Override
      protected RatpackServer createServer() throws Exception {
        return server.create();
      }
    };
  }

  /**
   * Creates an embedded application using the given server config, and server creating function.
   *
   * @param serverConfig the server configuration
   * @param definition a function to create the server to embed
   * @return a newly created embedded application
   */
  static EmbeddedApp fromServer(ServerConfigBuilder serverConfig, Action<? super RatpackServerSpec> definition) {
    return fromServer(serverConfig.build(), definition);
  }

  /**
   * Creates an embedded application using the given server config, and server creating function.
   *
   * @param serverConfig the server configuration
   * @param definition a function to create the server to embed
   * @return a newly created embedded application
   */
  static EmbeddedApp fromServer(ServerConfig serverConfig, Action<? super RatpackServerSpec> definition) {
    return fromServer(uncheck(() -> RatpackServer.of(b -> definition.execute(b.serverConfig(serverConfig)))));
  }

  /**
   * Creates an embedded application with a default launch config (no base dir, ephemeral port) and the given handler.
   * <p>
   * If you need to tweak the server config, use {@link #fromServer(ServerConfig, Action)}.
   *
   * @param handlerFactory a handler factory
   * @return a newly created embedded application
   */
  static EmbeddedApp fromHandlerFactory(Function<? super Registry, ? extends Handler> handlerFactory) {
    return fromServer(ServerConfig.embedded().build(), b -> b.handler(handlerFactory));
  }

  /**
   * Creates an embedded application with a default launch config (no base dir, ephemeral port) and the given handler.
   * <p>
   * If you need to tweak the server config, use {@link #fromServer(ServerConfig, Action)}.
   *
   * @param handler the application handler
   * @return a newly created embedded application
   */
  static EmbeddedApp fromHandler(Handler handler) {
    return fromServer(ServerConfig.embedded().build(), b -> b.handler(r -> handler));
  }

  /**
   * Creates an embedded application with a default launch config (no base dir, ephemeral port) and the given handler chain.
   * <p>
   * If you need to tweak the server config, use {@link #fromServer(ServerConfig, Action)}.
   *
   * @param action the handler chain definition
   * @return a newly created embedded application
   */
  static EmbeddedApp fromHandlers(Action<? super Chain> action) {
    return fromServer(ServerConfig.embedded().build(), b -> b.handler(r -> Handlers.chain(r.get(ServerConfig.class), r, action)));
  }

  /**
   * The server for the application.
   * <p>
   * Calling this method does not implicitly start the server.
   *
   * @return The server for the application
   */
  RatpackServer getServer();

  /**
   * {@inheritDoc}
   */
  @Override
  default URI getAddress() {
    RatpackServer server = getServer();
    try {
      if (!server.isRunning()) {
        server.start();
      }
      return new URI(server.getScheme(), null, server.getBindHost(), server.getBindPort(), "/", null, null);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Stops the server returned by {@link #getServer()}.
   * <p>
   * Exceptions thrown by calling {@link RatpackServer#stop()} are suppressed and written to {@link System#err System.err}.
   */
  @Override
  default void close() {
    try {
      getServer().stop();
    } catch (Exception e) {
      LoggerFactory.getLogger(this.getClass()).error("", e);
    }
  }

}
