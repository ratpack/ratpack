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
import ratpack.configuration.internal.DefaultConfigurationFactoryFactory;
import ratpack.configuration.internal.DefaultConfigurationSource;
import ratpack.configuration.util.internal.Generics;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchException;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerBuilder;

import java.util.Properties;

/**
 * An application entry point for starting a Ratpack application.
 * <p>
 * To create a new application, extend this class, specifying your {@link Configuration} subclass and adding a {@code main} method
 * that calls {@link #startOrExit()}
 */
public abstract class Application<T extends Configuration> {
  private final static Logger LOGGER = LoggerFactory.getLogger(Application.class);

  protected final Class<T> getConfigurationClass() {
      return Generics.getTypeParameter(getClass(), Configuration.class);
  }

  /**
   * Starts the server.
   *
   * @throws Exception if the server cannot be started
   */
  public void start() throws Exception {
    try {
      Properties overrideProperties = System.getProperties();
      Properties defaultProperties = new Properties();
      addImpliedDefaults(defaultProperties);
      ConfigurationSource configurationSource = new DefaultConfigurationSource(overrideProperties, defaultProperties);
      ConfigurationFactoryFactory configurationFactoryFactory = new DefaultConfigurationFactoryFactory();
      try {
        ConfigurationFactory configurationFactory = configurationFactoryFactory.build(configurationSource);
        T configuration = configurationFactory.build(getConfigurationClass(), configurationSource);
        LaunchConfig launchConfig = configuration.getLaunchConfigFactory().build(configurationSource, configuration);
        RatpackServer server = RatpackServerBuilder.build(launchConfig);
        server.start();
      } catch (ConfigurationException ex) {
        throw new LaunchException("Failed to launch application", ex);
      }
    } catch (Exception e) {
      LOGGER.error("", e);
      System.exit(1);
    }
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

  /**
   * Subclass hook for adding default property values.
   * <p>
   * This implementation does not add any.
   *
   * @param properties The properties to add the defaults to
   */
  protected void addImpliedDefaults(Properties properties) { }
}
