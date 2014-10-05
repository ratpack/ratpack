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
 * Interface for configuration provider discovery.
 */
public interface ConfigurationFactoryFactory {
  /**
   * Builds a configuration factory based on a configuration source.
   *
   * @param configurationSource the configuration source
   * @return the built configuration factory
   * @throws ConfigurationException if there's an error building the configuration factory
   */
  ConfigurationFactory build(ConfigurationSource configurationSource) throws ConfigurationException;
}
