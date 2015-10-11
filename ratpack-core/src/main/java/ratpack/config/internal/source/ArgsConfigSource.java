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

package ratpack.config.internal.source;

import com.google.common.base.Splitter;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class ArgsConfigSource extends AbstractPropertiesConfigSource {

  private final String separator;
  private final String[] args;

  public ArgsConfigSource(String prefix, String separator, String[] args) {
    super(Optional.ofNullable(prefix));
    this.separator = separator;
    this.args = args;
  }

  @Override
  protected Properties loadProperties() throws Exception {
    Splitter splitter = Splitter.on(separator).limit(2);
    Properties properties = new Properties();
    for (String arg : args) {
      List<String> values = splitter.splitToList(arg);
      if (values.size() == 1) {
        properties.put(values.get(0), "");
      } else {
        properties.put(values.get(0), values.get(1));
      }
    }
    return properties;
  }
}
