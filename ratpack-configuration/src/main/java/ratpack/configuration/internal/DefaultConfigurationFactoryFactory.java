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

import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.Iterables;
import ratpack.configuration.ConfigurationException;
import ratpack.configuration.ConfigurationFactory;
import ratpack.configuration.ConfigurationFactoryFactory;
import ratpack.configuration.ConfigurationSource;
import ratpack.launch.internal.LaunchConfigsInternal;
import ratpack.util.internal.TypeCoercingProperties;

import java.util.ServiceLoader;

import static ratpack.launch.LaunchConfigs.Property.CONFIGURATION_FACTORY;

/**
 * A default configuration factory factory.
 * It attempts to locate the desired configuration factory by way of a {@link java.util.ServiceLoader}, falling back to a default.
 * If that doesn't produce the desired result, configuration property {@value ratpack.launch.LaunchConfigs.Property#CONFIGURATION_FACTORY} can be used to specify the desired configuration factory.
 */
public class DefaultConfigurationFactoryFactory implements ConfigurationFactoryFactory {
  private final ClassLoader classLoader;

  public DefaultConfigurationFactoryFactory(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public ConfigurationFactory build(ConfigurationSource configurationSource) throws ConfigurationException {
    TypeCoercingProperties props = LaunchConfigsInternal.consolidatePropertiesFromGlobalProperties(
      StandardSystemProperty.USER_DIR.value(), classLoader, configurationSource.getOverrideProperties(), configurationSource.getDefaultProperties());
    try {
      Class<ConfigurationFactory> configurationFactoryClass = props.asClass(CONFIGURATION_FACTORY, ConfigurationFactory.class);
      if (configurationFactoryClass != null) {
        return configurationFactoryClass.newInstance();
      }
    } catch (ReflectiveOperationException ex) {
      throw new ConfigurationException("Could not instantiate specified configuration factory class " + props.asString(CONFIGURATION_FACTORY, null), ex);
    }
    ServiceLoader<ConfigurationFactory> serviceLoader = ServiceLoader.load(ConfigurationFactory.class, classLoader);
    try {
      return Iterables.getOnlyElement(serviceLoader, new DefaultConfigurationFactory());
    } catch (IllegalArgumentException ex) {
      throw new ConfigurationException("Multiple possible configuration factories were found; please specify one with the '" + CONFIGURATION_FACTORY + "' property", ex);
    }
  }
}
