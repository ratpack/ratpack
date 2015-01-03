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
import ratpack.func.Action;

import java.net.URL;
import java.nio.file.Path;
import java.util.Properties;

public interface ConfigurationDataSpec {
  ConfigurationDataSpec configureObjectMapper(Action<ObjectMapper> action);

  ConfigurationDataSpec add(ConfigurationSource configurationSource);

  ConfigurationData build();

  ConfigurationDataSpec env();

  // TODO: consider adding support for ByteSource arguments

  ConfigurationDataSpec json(Path path);

  ConfigurationDataSpec json(URL url);

  ConfigurationDataSpec props(Path path);

  ConfigurationDataSpec props(Properties properties);

  ConfigurationDataSpec props(URL url);

  ConfigurationDataSpec sysProps();

  ConfigurationDataSpec yaml(Path path);

  ConfigurationDataSpec yaml(URL url);
}
