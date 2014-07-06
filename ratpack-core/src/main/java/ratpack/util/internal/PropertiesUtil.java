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

package ratpack.util.internal;

import java.util.Map;
import java.util.Properties;

public class PropertiesUtil {
  public static void extractProperties(String propertyPrefix, Properties properties, Map<? super String, ? super String> destination) {
    for (String propertyName : properties.stringPropertyNames()) {
      if (propertyName.startsWith(propertyPrefix)) {
        destination.put(propertyName.substring(propertyPrefix.length()), properties.getProperty(propertyName));
      }
    }
  }
}
