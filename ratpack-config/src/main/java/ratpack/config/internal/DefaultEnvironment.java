/*
 * Copyright 2020 the original author or authors.
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

package ratpack.config.internal;

import ratpack.config.Environment;

import java.util.Map;
import java.util.Properties;

public class DefaultEnvironment implements Environment {

  private final Map<String, String> env;
  private final Properties properties;

  public DefaultEnvironment() {
    this(System.getenv(), System.getProperties());
  }

  public DefaultEnvironment(Map<String, String> env, Properties properties) {
    this.env = env;
    this.properties = properties;
  }

  @Override
  public Map<String, String> getenv() {
    return env;
  }

  @Override
  public Properties getProperties() {
    return properties;
  }
}
