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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * A {@link BaseDirBuilder} implementation that uses a Jar file as the base.
 * <p>
 * Using this base dir builder simulates fat jar deployment.
 * Extensions or add-ons that utilise the file system should test their functionality with this base dir
 * to ensure that they do not assume access to the naked file system.
 * <pre class="tested">
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.launch.HandlerFactory;
 * import ratpack.launch.LaunchConfig;
 * import ratpack.launch.LaunchConfigBuilder;
 * import ratpack.test.embed.EmbeddedApplication;
 * import ratpack.test.embed.LaunchConfigEmbeddedApplication;
 * import ratpack.test.embed.BaseDirBuilder;
 * import ratpack.test.embed.JarFileBaseDirBuilder;
 *
 * import java.io.File;
 * import java.net.HttpURLConnection;
 * import java.net.URI;
 * import java.net.URLConnection;
 * import java.nio.file.Files;
 *
 * File tmp = Files.createTempDirectory("ratpack-test").toFile();
 *
 * BaseDirBuilder baseDir = new JarFileBaseDirBuilder(new File(tmp, "the.jar"));
 *
 * EmbeddedApplication application = new LaunchConfigEmbeddedApplication() {
 *   protected LaunchConfig createLaunchConfig() {
 *     return LaunchConfigBuilder.
 *       baseDir(Files.createTempDirectory("ratpack-test")).
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
 * application.close();
 * </pre>
 */
public class JarFileBaseDirBuilder extends PathBaseDirBuilder {

  public JarFileBaseDirBuilder(File jar) {
    super(getJarPath(jar));
  }

  private static Path getJarPath(File jar) {
    URI uri = URI.create("jar:" + jar.toURI().toString());
    Map<String, String> env = new HashMap<>();
    env.put("create", "true");
    FileSystem fileSystem;
    try {
      fileSystem = FileSystems.newFileSystem(uri, env);
    } catch (IOException e) {
      throw uncheck(e);
    }
    return fileSystem.getPath("/");
  }

}
