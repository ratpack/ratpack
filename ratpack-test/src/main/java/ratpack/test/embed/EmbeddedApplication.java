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

import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.server.RatpackServer;
import ratpack.test.ApplicationUnderTest;
import ratpack.test.http.TestHttpClient;

import java.util.function.Consumer;

import static ratpack.util.ExceptionUtils.uncheck;

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
 *
 * @see ratpack.test.embed.LaunchConfigEmbeddedApplication
 */
public interface EmbeddedApplication extends ApplicationUnderTest, AutoCloseable {

  /**
   * Creates an embedded application by building a {@link LaunchConfig}.
   * <p>
   * The given {@link LaunchConfigBuilder} will be configured to not have base dir, and to use an ephemeral port.
   *
   * @param function a function that builds a launch config from a launch config builder
   * @return a newly created embedded application
   */
  static EmbeddedApplication fromLaunchConfigBuilder(Function<? super LaunchConfigBuilder, ? extends LaunchConfig> function) {
    return new LaunchConfigEmbeddedApplication() {
      @Override
      protected LaunchConfig createLaunchConfig() {
        return uncheck(() -> function.apply(LaunchConfigBuilder.noBaseDir().port(0)));
      }
    };
  }

  /**
   * Creates an embedded application with a default launch config (no base dir, ephemeral port) and the given handler.
   * <p>
   * If you need to tweak the launch config, use {@link #fromLaunchConfigBuilder(Function)}.
   *
   * @param handler the application handler
   * @return a newly created embedded application
   */
  static EmbeddedApplication fromHandler(Handler handler) {
    return fromLaunchConfigBuilder(lcb -> lcb.build(lc -> handler));
  }

  /**
   * Creates an embedded application with a default launch config (no base dir, ephemeral port) and the given handler chain.
   * <p>
   * If you need to tweak the launch config, use {@link #fromLaunchConfigBuilder(Function)}.
   *
   * @param action the handler chain definition
   * @return a newly created embedded application
   */
  static EmbeddedApplication fromChain(Action<? super Chain> action) {
    return fromLaunchConfigBuilder(lcb -> lcb.build(lc -> Handlers.chain(lc, action)));
  }

  /**
   * Provides the given consumer with a {@link #getHttpClient() test http client} for this application, then closes this application.
   * <p>
   * The application will be closed regardless of whether the given consumer throws an exception.
   * <pre class="java">
   *
   * import ratpack.test.embed.EmbeddedApplication;
   *
   * public class Example {
   *   public static void main(String... args) {
   *     EmbeddedApplication.fromHandler(ctx -> ctx.render("ok"))
   *       .test(httpClient -> {
   *         assert httpClient.get().getBody().getText().equals("ok");
   *       });
   *   }
   * }
   * </pre>
   *
   * @param consumer a consumer that tests this embedded application
   */
  default void test(Consumer<? super TestHttpClient> consumer) {
    try {
      consumer.accept(getHttpClient());
    } finally {
      close();
    }
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
   * Creates a new test HTTP client that tests this embedded application.
   *
   * @return a new test HTTP client that tests this embedded application
   */
  TestHttpClient getHttpClient();

  @Override
  void close();

}
