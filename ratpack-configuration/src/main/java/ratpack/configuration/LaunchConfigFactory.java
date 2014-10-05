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

import ratpack.launch.LaunchConfig;

/**
 * A factory for LaunchConfig instances based on a configuration.
 */
public interface LaunchConfigFactory {
  /**
   * Builds a LaunchConfig based on the provided configuration data.
   *
   * @param configurationSource the configuration source
   * @param configuration the configuration
   * @return the built LaunchConfig
   * @throws ConfigurationException if there is an error building the LaunchConfig
   */
  LaunchConfig build(ConfigurationSource configurationSource, Configuration configuration) throws ConfigurationException;
}
