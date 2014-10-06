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
import ratpack.configuration.ConfigurationException;
import ratpack.configuration.ConfigurationFactory;
import ratpack.configuration.ConfigurationSource;

/**
 * A simple configuration factory that just instantiates the desired configuration class, and completely ignores the configuration source.
 */
public class DefaultConfigurationFactory implements ConfigurationFactory {
  @Override
  public <T extends Configuration> T build(Class<T> configurationClass, ConfigurationSource configurationSource) throws ConfigurationException {
    try {
      return configurationClass.newInstance();
    } catch (ReflectiveOperationException ex) {
      throw new ConfigurationException("Could not instantiate configuration class " + configurationClass.getName(), ex);
    }
  }
}
