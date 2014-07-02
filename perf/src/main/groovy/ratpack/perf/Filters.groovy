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

package ratpack.perf

import groovy.util.logging.Slf4j

@Slf4j
class Filters {

  private final Map<String, List<String>> data

  Filters(Map<String, List<String>> data) {
    this.data = data
  }

  boolean testApp(String appName) {
    data.isEmpty() || appName in data.keySet()
  }

  boolean testEndpoint(String appName, String endpointName) {
    data.isEmpty() || (testApp(appName) && (endpointName in data[appName] || "*" in data[appName]))
  }

}
