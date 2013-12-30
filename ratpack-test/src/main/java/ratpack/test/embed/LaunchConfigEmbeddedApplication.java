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

import ratpack.launch.LaunchConfig;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerBuilder;

import java.io.File;

/**
 * A supporting implementation of {@link EmbeddedApplication} that starts a server based on a subclass provided {@link LaunchConfig}.
 * <p>
 * <pre class="tested">
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.launch.HandlerFactory;
 * import ratpack.launch.LaunchConfig;
 * import ratpack.launch.LaunchConfigBuilder;
 * import ratpack.test.embed.EmbeddedApplication;
 * import ratpack.test.embed.LaunchConfigEmbeddedApplication;
 *
 * import java.io.File;
 * import java.net.HttpURLConnection;
 * import java.net.URI;
 * import java.net.URLConnection;
 * import java.nio.file.Files;
 *
 * File baseDir = Files.createTempDirectory("ratpack-test").toFile();
 * EmbeddedApplication application = new LaunchConfigEmbeddedApplication(baseDir) {
 *   protected LaunchConfig createLaunchConfig() {
 *     return LaunchConfigBuilder.
 *       baseDir(getBaseDir()).
 *       port(0).
 *       build(new HandlerFactory() {
 *         public Handler create(LaunchConfig launchConfig) {
 *           return new Handler() {
 *             public void handle(Context context) {
 *               context.getResponse().status(200).send();
 *             }
 *           };
 *         }
 *       });
 *   }
 * };
 *
 * URI address = application.getAddress();
 * HttpURLConnection urlConnection = (HttpURLConnection) address.toURL().openConnection();
 * urlConnection.connect();
 *
 * assert urlConnection.getResponseCode() == 200;
 *
 * application.getServer().stop();
 * </pre>
 */
public abstract class LaunchConfigEmbeddedApplication extends EmbeddedApplicationSupport {

  /**
   * Constructor.
   *
   * @param baseDir The base dir
   */
  public LaunchConfigEmbeddedApplication(File baseDir) {
    super(baseDir);
  }

  /**
   * Creates a server using {@link RatpackServerBuilder#build(LaunchConfig)}, using the launch config returned by {@link #createLaunchConfig()}.
   *
   * @return The server to test
   */
  @Override
  protected RatpackServer createServer() {
    LaunchConfig launchConfig = createLaunchConfig();
    return RatpackServerBuilder.build(launchConfig);
  }

  /**
   * Creates a launch config that defines the application.
   * <p>
   * Will be called only once.
   *
   * @return a launch config that defines the application
   */
  abstract protected LaunchConfig createLaunchConfig();

}
