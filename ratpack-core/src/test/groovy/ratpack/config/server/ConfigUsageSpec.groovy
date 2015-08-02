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

package ratpack.config.server

import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import ratpack.server.ServerConfig
import spock.lang.Specification

import java.nio.file.Path

import static com.google.common.base.Charsets.UTF_8

abstract class ConfigUsageSpec extends Specification {
  protected static ConfigData noData() {
    ConfigData.of {}
  }

  protected static ServerConfig yamlConfig(Path baseDir, String data) {
    ServerConfig.of { it.baseDir(baseDir).yaml(toByteSource(data)) }
  }

  protected static ByteSource toByteSource(String data) {
    ByteSource.wrap(data.getBytes(UTF_8))
  }
}
