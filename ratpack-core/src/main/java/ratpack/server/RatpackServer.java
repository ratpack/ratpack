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
import ratpack.impose.Impositions;
import ratpack.registry.Registry;
import ratpack.server.internal.DefaultRatpackServer;

import java.util.Optional;

/**
 * The entry point for creating and starting a Ratpack application.
 * <p>
 * The {@link #of(Action)} static method is used to create a server.
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 *
 * import ratpack.test.ServerBackedApplicationUnderTest;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     RatpackServer server = RatpackServer.of(s -> s
 *       .serverConfig(ServerConfig.embedded())            // base server configuration (e.g. port) - optional
 *       .registryOf(r -> r.add(String.class, "foo"))      // registry of supporting objects - optional
 *       .handlers(chain -> chain                          // request handlers - required
 *         .get("a", ctx -> ctx.render(ctx.get(String.class) + " 1"))
 *         .get("b", ctx -> ctx.render(ctx.get(String.class) + " 2"))
 *       )
 *     );
 *
 *     // this method starts the server, via server.start() and then calls server.stop()
 *     ServerBackedApplicationUnderTest.of(server).test(httpClient -> {
 *       assertEquals("foo 1", httpClient.getText("a"));
 *       assertEquals("foo 2", httpClient.getText("b"));
 *     });
 *   }
 * }
 * }</pre>
 *
 * Server objects are thread safe in that every instance method is {@code synchronized}.
 *
 * @see RatpackServerSpec
 * @see ratpack.service.Service
 * @see #of(Action)
 */
public interface RatpackServer {

  // TODO document the contents of the server registry, and the way that the user registry is joined on to it

  /**
   * Creates a new, unstarted, Ratpack server from the given definition.
   * <p>
   * The action argument effectively serves as the definition of the server.
   * It receives a mutable server builder style object, a {@link RatpackServerSpec}.
   * The action is retained internally by the server, and invoked again if the {@link #reload()} method is called.
   *
   * @param definition the server definition
   * @return a Ratpack server
   * @see RatpackServerSpec
   * @throws Exception any thrown by creating the server
   */
  static RatpackServer of(Action<? super RatpackServerSpec> definition) throws Exception {
    return new DefaultRatpackServer(definition, Impositions.current());
  }

  /**
   * Convenience method to {@link #of(Action) define} and {@link #start()} the server in one go.
   *
   * @param definition the server definition
   * @return the newly created and started server
   * @throws Exception any thrown by {@link #of(Action)} or {@link #start()}
   */
  static RatpackServer start(Action<? super RatpackServerSpec> definition) throws Exception {
    RatpackServer server = of(definition);
    server.start();
    return server;
  }

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
   *     RatpackServer server = RatpackServer.of(s -> s
   *       .serverConfig(ServerConfig.embedded())
   *       .registry(r -> r.add(String.class, holder[0]))
   *       .all(registry -> (ctx) -> ctx.render(ctx.get(String.class)))
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
   * Convenience method to provide easy access to the application registry via a server reference
   *
   * @return a Ratpack registry
   * @since 1.6
   */
  Optional<Registry> getRegistry();
}
