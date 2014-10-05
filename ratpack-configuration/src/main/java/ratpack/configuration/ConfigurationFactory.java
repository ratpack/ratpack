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

/**
 * Interface for configuration providers.
 * You can add a custom configuration provider by registering it via the {@link java.util.ServiceLoader} mechanism,
 * or by specifying the property {@value ratpack.configuration.internal.DefaultConfigurationFactoryFactory#CONFIGURATION_FACTORY} in your {@code ratpack.properties}.
 */
public interface ConfigurationFactory {
  /**
   * Builds a configuration based on a configuration source.
   *
   * @param configurationClass the configuration class
   * @param configurationSource the configuration source
   * @param <T> the type of the configuration class
   * @return the built configuration
   * @throws ConfigurationException if there's an error building the configuration
   */
  <T extends Configuration> T build(Class<T> configurationClass, ConfigurationSource configurationSource) throws ConfigurationException;
}
