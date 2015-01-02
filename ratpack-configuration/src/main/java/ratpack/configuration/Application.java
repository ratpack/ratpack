/*
 * Copyright 2014 the original author or authors.
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

package ratpack.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.launch.*;

/**
 * An application entry point for starting a Configuration-based Ratpack application.
 */
public class Application {
  private final static Logger LOGGER = LoggerFactory.getLogger(Application.class);

  /**
   * Starts the server.
   *
   * @throws Exception if the server cannot be started
   */
  public void start() throws Exception {
    try {
      //TODO-JOHN
      LaunchConfig launchConfig = buildLaunchConfig();
      RatpackLauncher.with(ServerConfigBuilder.launchConfig(launchConfig).build())
        .build(launchConfig.getHandlerFactory()).start();
    } catch (ConfigurationException ex) {
      throw new LaunchException("Failed to launch application", ex);
    }
  }

  protected LaunchConfig buildLaunchConfig() {
    return new ConfigurationLaunchConfigBuilder().build();
  }

  /**
   * Starts the server via {@link #start()}, exiting via {@code System.exit(1)} if that method throws an exception.
   */
  public void startOrExit() {
    try {
      start();
    } catch (Exception e) {
      LOGGER.error("", e);
      System.exit(1);
    }
  }

  public static void main(String... args) {
    new Application().startOrExit();
  }
}
