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

package ratpack.configuration.internal;

import ratpack.configuration.Configuration;
import ratpack.configuration.ConfigurationSource;
import ratpack.configuration.LaunchConfigFactory;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigs;
import ratpack.registry.Registries;
import ratpack.registry.Registry;

/**
 * A default LaunchConfigFactory that delegates to {@link ratpack.launch.LaunchConfigs} after putting the configuration class in the default registry.
 */
public class DefaultLaunchConfigFactory implements LaunchConfigFactory {
  private final ClassLoader classLoader;

  public DefaultLaunchConfigFactory(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public LaunchConfig build(ConfigurationSource configurationSource, Configuration configuration) {
    Registry registry = Registries.registry().add(Configuration.class, configuration).add(configuration).build();
    return LaunchConfigs.createFromGlobalProperties(classLoader, configurationSource.getOverrideProperties(), configurationSource.getDefaultProperties(), registry);
  }
}
