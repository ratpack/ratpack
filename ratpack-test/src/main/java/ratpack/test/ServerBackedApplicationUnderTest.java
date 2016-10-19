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

package ratpack.test;

import ratpack.func.Factory;
import ratpack.impose.ForceDevelopmentImposition;
import ratpack.impose.ForceServerListenPortImposition;
import ratpack.impose.Impositions;
import ratpack.impose.ImpositionsSpec;
import ratpack.server.RatpackServer;

import java.net.URI;
import java.net.URISyntaxException;

import static ratpack.util.Exceptions.uncheck;

/**
 * An {@link ApplicationUnderTest} implementation that manages a {@link RatpackServer}.
 * <p>
 * This class can be used in tests to handle starting the server, making HTTP requests to it and shutting it down when done.
 * It is typically used in tests where the actual application is available on the classpath.
 * <p>
 * Implementations need only provide a {@link #createServer()} method.
 * <p>
 * Closing this application under test will {@link RatpackServer#stop() stop the server}.
 * Users should ensure that objects of this type are closed when done with, to release ports and other resources.
 * This is typically done in a test cleanup method, such as via JUnit's {@code @After} mechanism.
 * <p>
 * This class supports {@link Impositions}, which can be used to augment the server for testability.
 *
 * @see MainClassApplicationUnderTest
 * @see #addImpositions(ImpositionsSpec)
 */
public abstract class ServerBackedApplicationUnderTest implements CloseableApplicationUnderTest {

  private RatpackServer server;

  /**
   * Creates a new instance backed by the given server.
   *
   * @param ratpackServer the server to test
   * @return an application under test backed by the given server
   * @since 1.2
   */
  public static ServerBackedApplicationUnderTest of(RatpackServer ratpackServer) {
    return of(Factory.constant(ratpackServer));
  }

  /**
   * Creates a new instance backed by the server returned by the given function.
   * <p>
   * The function is called lazily, the first time the server is needed.
   *
   * @param ratpackServer the server to test
   * @return an application under test backed by the given server
   * @since 1.2
   */
  public static ServerBackedApplicationUnderTest of(Factory<? extends RatpackServer> ratpackServer) {
    return new ServerBackedApplicationUnderTest() {
      @Override
      protected RatpackServer createServer() throws Exception {
        return ratpackServer.create();
      }
    };
  }

  /**
   * Creates the server to be tested.
   * <p>
   * The server does not need to be started when returned by this method.
   *
   * @return the server to test
   * @throws Exception any
   */
  protected abstract RatpackServer createServer() throws Exception;

  /**
   * Creates the {@link Impositions} to impose on the server.
   * <p>
   * This implementation effectively delegates to {@link #addDefaultImpositions(ImpositionsSpec)} and {@link #addImpositions(ImpositionsSpec)}.
   * <p>
   * It is generally more appropriate to override {@link #addImpositions(ImpositionsSpec)} than this method.
   *
   * @return the impositions
   * @throws Exception any
   * @since 1.2
   */
  protected Impositions createImpositions() throws Exception {
    return Impositions.of(i -> {
      addDefaultImpositions(i);
      addImpositions(i);
    });
  }

  /**
   * Adds default impositions, that make sense in most cases.
   * <p>
   * Specifically adds a {@link ForceDevelopmentImposition} with a {@code true} value,
   * and a {@link ForceServerListenPortImposition#ephemeral()} imposition.
   * <p>
   * To negate or change the default impositions, simply add a different imposition of the same type in {@link #addImpositions(ImpositionsSpec)}.
   * Doing so will overwrite the existing imposition of the same type, set by this method.
   * <p>
   * It is generally not necessary to override this method.
   *
   * @param impositionsSpec the impositions spec, that impositions can be added to
   * @since 1.2
   */
  protected void addDefaultImpositions(ImpositionsSpec impositionsSpec) {
    impositionsSpec.add(ForceServerListenPortImposition.ephemeral());
    impositionsSpec.add(ForceDevelopmentImposition.of(true));
  }

  /**
   * Adds impositions to be imposed on the server while it is being created and starting.
   *
   * <pre class="java">{@code
   * import ratpack.server.RatpackServer;
   * import ratpack.impose.ImpositionsSpec;
   * import ratpack.impose.ServerConfigImposition;
   * import ratpack.test.MainClassApplicationUnderTest;
   *
   * import static java.util.Collections.singletonMap;
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static class App {
   *     public static void main(String[] args) throws Exception {
   *       RatpackServer.start(s -> s
   *         .serverConfig(c -> c
   *           .props(singletonMap("string", "foo"))
   *           .require("/string", String.class)
   *         )
   *         .handlers(c -> c
   *           .get(ctx -> ctx.render(ctx.get(String.class)))
   *         )
   *       );
   *     }
   *   }
   *
   *   public static void main(String[] args) throws Exception {
   *     new MainClassApplicationUnderTest(App.class) {
   *       {@literal @}Override
   *       protected void addImpositions(ImpositionsSpec impositions) {
   *         impositions.add(ServerConfigImposition.of(c ->
   *           c.props(singletonMap("string", "bar"))
   *         ));
   *       }
   *     }.test(testHttpClient ->
   *       assertEquals("bar", testHttpClient.getText())
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param impositions the spec to add impositions to
   * @see Impositions
   * @see ratpack.impose.ServerConfigImposition
   * @see ratpack.impose.ForceDevelopmentImposition
   * @see ratpack.impose.ForceServerListenPortImposition
   * @see ratpack.impose.UserRegistryImposition
   * @since 1.2
   */
  protected void addImpositions(ImpositionsSpec impositions) {

  }

  /**
   * Returns the address to the root of the server, starting it if necessary.
   *
   * @return the address to the root of the server
   */
  @Override
  public URI getAddress() {
    if (server == null) {
      try {
        server = createImpositions().impose(() -> {
          RatpackServer server = createServer();
          server.start();
          return server;
        });
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    URI address;
    try {
      address = new URI(server.getScheme() + "://" + server.getBindHost() + ":" + server.getBindPort() + "/");
    } catch (URISyntaxException e) {
      throw uncheck(e);
    }

    return address;
  }

  /**
   * Stops the server if it is running.
   *
   * @see RatpackServer#stop()
   */
  public void stop() {
    if (server != null) {
      try {
        server.stop();
        server = null;
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

  }

  /**
   * Delegates to {@link #stop()}.
   */
  public void close() {
    stop();
  }
}
