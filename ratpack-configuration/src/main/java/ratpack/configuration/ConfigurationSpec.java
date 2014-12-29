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

import ratpack.configuration.internal.*;

/**
 * An additive specification for configurations
 */
public interface ConfigurationSpec {

  /**
   * Back this configuration with values from a file located in the classpath.
   * @param path
   * @return
   */
  default ConfigurationSpec load(String path) {
    return new HierarchialConfiguration(new ClasspathBackedConfiguration(path), this);
  }

  /**
   * Back this configuration with values obtained from System Properties.
   * @return
   */
  default ConfigurationSpec sysProps() {
    return new HierarchialConfiguration(new SystemPropertiesBackedConfiguration(), this);
  }

  /**
   * Back this configuration with values obtained from environment variables.
   * @return
   */
  default ConfigurationSpec envvars() {
    return new HierarchialConfiguration(new EnvironmentVariablesBackedConfiguration(), this);
  }

  /**
   * Back this configuration with values obtained from a file on the system.
   * @param path
   * @return
   */
  default ConfigurationSpec fileSystem(String path) {
    return new HierarchialConfiguration(new FileBackedConfiguration(path), this);
  }

  /**
   * Compose the final configuration from all sources.
   * @return
   */
  Configuration build();
}
