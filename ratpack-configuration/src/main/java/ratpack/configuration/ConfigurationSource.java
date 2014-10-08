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

import com.google.common.io.ByteSource;

import java.util.Properties;

/**
 * A source of configuration data.
 */
public interface ConfigurationSource {
  /**
   * A class loader that can be used to load configuration-related data.
   */
  ClassLoader getClassLoader();

  /**
   * A source of configuration data, often from a file.  May be empty.
   * It's up to the {@link ratpack.configuration.ConfigurationFactory} to determine how to interpret this data.
   *
   * @return a source of configuration data
   */
  ByteSource getByteSource();

  /**
   * Properties specified at launch time that should override the default values.
   *
   * @return the override properties
   * @see ratpack.launch.LaunchConfigs
   */
  Properties getOverrideProperties();

  /**
   * Default properties to configure the application.
   *
   * @return the default properties
   * @see ratpack.launch.LaunchConfigs
   */
  Properties getDefaultProperties();
}
