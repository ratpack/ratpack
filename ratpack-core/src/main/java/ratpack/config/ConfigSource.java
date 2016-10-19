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
import com.fasterxml.jackson.databind.node.ObjectNode;
import ratpack.file.FileSystemBinding;

/**
 * Allows providing custom sources of configuration data.
 *
 * @see ConfigDataBuilder#add(ConfigSource)
 */
@FunctionalInterface
public interface ConfigSource {
  /**
   * Loads the configuration data from this data source.
   *
   * @param fileSystemBinding the file system view the config source must use to find files
   * @param objectMapper the Jackson ObjectMapper to use to build objects
   * @return the root node of the configuration data loaded
   * @throws Exception any
   */
  ObjectNode loadConfigData(ObjectMapper objectMapper, FileSystemBinding fileSystemBinding) throws Exception;
}
