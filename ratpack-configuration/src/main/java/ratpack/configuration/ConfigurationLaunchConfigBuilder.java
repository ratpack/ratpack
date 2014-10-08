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

import com.google.common.base.StandardSystemProperty;
import com.google.common.io.ByteSource;
import ratpack.configuration.internal.DefaultConfigurationFactoryFactory;
import ratpack.configuration.internal.DefaultConfigurationSource;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigs;
import ratpack.launch.internal.LaunchConfigsInternal;
import ratpack.util.internal.TypeCoercingProperties;

import java.util.Properties;

/**
 * Builds a {@link ratpack.launch.LaunchConfig} by using the configuration system.
 */
public class ConfigurationLaunchConfigBuilder {
  private String workingDir = StandardSystemProperty.USER_DIR.value();
  private ConfigurationSource configurationSource;
  private ClassLoader classLoader = Application.class.getClassLoader();
  private ByteSource byteSource;
  private Properties overrideProperties = System.getProperties();
  private Properties defaultProperties = new Properties();
  private Class<? extends Configuration> configurationClass;

  /**
   * Sets the working dir.
   *
   * @param workingDir the working dir
   * @return this
   */
  public ConfigurationLaunchConfigBuilder workingDir(String workingDir) {
    this.workingDir = workingDir;
    return this;
  }

  /**
   * Sets the configuration source.
   *
   * @param configurationSource the configuration source
   * @return this
   */
  public ConfigurationLaunchConfigBuilder configurationSource(ConfigurationSource configurationSource) {
    this.configurationSource = configurationSource;
    return this;
  }

  /**
   * Sets the class loader to use for a default configuration source.
   *
   * @param classLoader the class loader
   * @return this
   */
  public ConfigurationLaunchConfigBuilder classLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  /**
   * Sets the byte source to use for a default configuration source.
   *
   * @param byteSource the byte source
   * @return this
   */
  public ConfigurationLaunchConfigBuilder byteSource(ByteSource byteSource) {
    this.byteSource = byteSource;
    return this;
  }

  /**
   * Sets the override properties to use for a default configuration source.
   *
   * @param overrideProperties the override properties
   * @return this
   */
  public ConfigurationLaunchConfigBuilder overrideProperties(Properties overrideProperties) {
    this.overrideProperties = overrideProperties;
    return this;
  }

  /**
   * Sets the default properties to use for a default configuration source.
   *
   * @param defaultProperties the default properties
   * @return this
   */
  public ConfigurationLaunchConfigBuilder defaultProperties(Properties defaultProperties) {
    this.defaultProperties = defaultProperties;
    return this;
  }

  /**
   * Sets the configuration class.
   *
   * @param configurationClass the configuration class
   * @return this
   */
  public ConfigurationLaunchConfigBuilder configurationClass(Class<? extends Configuration> configurationClass) {
    this.configurationClass = configurationClass;
    return this;
  }

  /**
   * Builds the launch config.
   *
   * @return the launch config
   * @throws ConfigurationException if there's an error building the launch config
   */
  public LaunchConfig build() throws ConfigurationException {
    TypeCoercingProperties props = LaunchConfigsInternal.consolidatePropertiesFromGlobalProperties(workingDir, classLoader, overrideProperties, defaultProperties);
    if (configurationSource == null) {
      if (byteSource == null) {
        byteSource = props.asByteSource(LaunchConfigs.Property.CONFIGURATION_FILE);
      }
      configurationSource = new DefaultConfigurationSource(classLoader, byteSource, overrideProperties, defaultProperties);
    }
    ConfigurationFactoryFactory configurationFactoryFactory = new DefaultConfigurationFactoryFactory(classLoader);
    ConfigurationFactory configurationFactory = configurationFactoryFactory.build(configurationSource);
    if (configurationClass == null) {
      try {
        configurationClass = props.asClass(LaunchConfigs.Property.CONFIGURATION_CLASS, Configuration.class);
      } catch (ClassNotFoundException ex) {
        throw new ConfigurationException("Could not load specified configuration class", ex);
      }
    }
    Configuration configuration = configurationFactory.build(configurationClass, configurationSource);
    return configuration.getLaunchConfigFactory().build(configurationSource, configuration);
  }
}
