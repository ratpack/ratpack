/*
 * Copyright 2015 the original author or authors.
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

package ratpack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteSource;
import ratpack.func.Action;

import java.net.URL;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configures how configuration data will be loaded and bound to objects.
 * Multiple data sources can be specified.
 * All specified data sources will be merged together to form the final configuration data.
 * If a given value exists in multiple data sources, the value from the last specified source will be used.
 */
public interface ConfigurationDataSpec {
  /**
   * Configures the object mapper used for binding configuration data to arbitrary objects.
   *
   * @param action an action to perform upon the object mapper
   * @return this
   */
  ConfigurationDataSpec configureObjectMapper(Action<ObjectMapper> action);

  /**
   * Adds a configuration source.
   *
   * @param configurationSource the configuration source to add
   * @return this
   */
  ConfigurationDataSpec add(ConfigurationSource configurationSource);

  /**
   * Builds the configuration data from this specification.
   *
   * @return the newly build configuration data
   */
  ConfigurationData build();

  /**
   * Adds a configuration source for environment variables starting with the prefix {@value ratpack.config.internal.source.EnvironmentVariablesConfigurationSource#DEFAULT_PREFIX}.
   *
   * @return this
   */
  ConfigurationDataSpec env();

  /**
   * Adds a configuration source for environment variables starting with the specified prefix.
   *
   * @param prefix the prefix which should be used to identify relevant environment variables;
   * the prefix will be removed before loading the data
   * @return this
   */
  ConfigurationDataSpec env(String prefix);

  /**
   * Adds a configuration source for a JSON file.
   *
   * @param byteSource the source of the JSON data
   * @return this
   */
  ConfigurationDataSpec json(ByteSource byteSource);

  /**
   * Adds a configuration source for a JSON file.
   *
   * @param path the source of the JSON data
   * @return this
   */
  ConfigurationDataSpec json(Path path);

  /**
   * Adds a configuration source for a JSON file.
   *
   * @param pathOrUrl the source of the JSON data; may be either a file path or URL
   * @return this
   */
  ConfigurationDataSpec json(String pathOrUrl);

  /**
   * Adds a configuration source for a JSON file.
   *
   * @param url the source of the JSON data
   * @return this
   */
  ConfigurationDataSpec json(URL url);

  /**
   * Adds a configuration source for a properties file.
   *
   * @param byteSource the source of the properties data
   * @return this
   */
  ConfigurationDataSpec props(ByteSource byteSource);

  /**
   * Adds a configuration source for a properties file.
   *
   * @param path the source of the properties data
   * @return this
   */
  ConfigurationDataSpec props(Path path);

  /**
   * Adds a configuration source for a properties object.
   *
   * @param properties the properties object
   * @return this
   */
  ConfigurationDataSpec props(Properties properties);

  /**
   * Adds a configuration source for a properties file.
   *
   * @param pathOrUrl the source of the properties data; may be either a file path or URL
   * @return this
   */
  ConfigurationDataSpec props(String pathOrUrl);

  /**
   * Adds a configuration source for a properties file.
   *
   * @param url the source of the properties data
   * @return this
   */
  ConfigurationDataSpec props(URL url);

  /**
   * Adds a configuration source for system properties starting with the prefix {@value ratpack.config.internal.source.SystemPropertiesConfigurationSource#DEFAULT_PREFIX}.
   *
   * @return this
   */
  ConfigurationDataSpec sysProps();

  /**
   * Adds a configuration source for system properties starting with the specified prefix.
   *
   * @param prefix the prefix which should be used to identify relevant system properties;
   * the prefix will be removed before loading the data
   * @return this
   */
  ConfigurationDataSpec sysProps(String prefix);

  /**
   * Adds a configuration source for a YAML file.
   *
   * @param byteSource the source of the YAML data
   * @return this
   */
  ConfigurationDataSpec yaml(ByteSource byteSource);

  /**
   * Adds a configuration source for a YAML file.
   *
   * @param path the source of the YAML data
   * @return this
   */
  ConfigurationDataSpec yaml(Path path);

  /**
   * Adds a configuration source for a YAML file.
   *
   * @param pathOrUrl the source of the YAML data; may be either a file path or URL
   * @return this
   */
  ConfigurationDataSpec yaml(String pathOrUrl);

  /**
   * Adds a configuration source for a YAML file.
   *
   * @param url the source of the YAML data
   * @return this
   */
  ConfigurationDataSpec yaml(URL url);
}
