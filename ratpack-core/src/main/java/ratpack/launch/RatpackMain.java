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

package ratpack.launch;

import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerBuilder;

import java.util.Properties;

/**
 * An application entry point for starting a Ratpack application.
 * <p>
 * This class is designed to be subclassable.
 */
public class RatpackMain {

  /**
   * Starts a Ratpack application, by creating a new instance of this class and calling {@link #startOrExit()}.
   * <p>
   * If the application fails to start, the JVM will exit via {@code System.exit(1)}.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    new RatpackMain().startOrExit();
  }

  /**
   * Builds a server by calling {@link LaunchConfigs#createFromGlobalProperties(ClassLoader, java.util.Properties, java.util.Properties)}.
   * <p>
   * Uses this class's classloader as the classloader.
   *
   * @param overrideProperties The override properties
   * @param defaultProperties The default properties
   * @return A ratpack server, built from the launch config
   */
  public RatpackServer server(Properties overrideProperties, Properties defaultProperties) {
    addImpliedDefaults(defaultProperties);
    LaunchConfig launchConfig = LaunchConfigs.createFromGlobalProperties(RatpackMain.class.getClassLoader(), overrideProperties, defaultProperties);
    return RatpackServerBuilder.build(launchConfig);
  }

  /**
   * Starts the server returned by calling {@link #server(java.util.Properties, java.util.Properties)}.
   * <p>
   * The system properties are given as the override properties, and an empty property set as the defaults.
   *
   * @throws Exception if the server cannot be started
   */
  public void start() throws Exception {
    server(System.getProperties(), new Properties()).start();
  }

  /**
   * Starts the server via {@link #start()}, exiting via {@code System.exit(1)} if that method throws an exception.
   */
  public void startOrExit() {
    try {
      start();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Subclass hook for adding default property values.
   * <p>
   * This implementation does not add any.
   *
   * @param properties The properties to add the defaults to
   */
  protected void addImpliedDefaults(Properties properties) {

  }

}
