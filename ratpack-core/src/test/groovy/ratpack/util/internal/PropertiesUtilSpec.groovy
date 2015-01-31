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

package ratpack.util.internal

import spock.lang.Specification

class PropertiesUtilSpec extends Specification {
  def "extracts matching properties after removing prefix"() {
    given:
    def properties = new Properties([
      "ratpack.handlerFactory": "app.AppHandlerFactory",
      "ratpack.port": "8080",
      "ratpack.publicAddress": "http://app.example.com",
      "app.name": "myapp",
      "app.version": "1.0.2"
    ])

    when:
    def allMap = extractToNewMap("", properties)
    def ratpackMap = extractToNewMap("ratpack.", properties)
    def appMap = extractToNewMap("app.", properties)

    then:
    allMap == properties
    ratpackMap == [
      "handlerFactory": "app.AppHandlerFactory",
      "port": "8080",
      "publicAddress": "http://app.example.com",
    ]
    appMap == [name: "myapp", version: "1.0.2"]
  }

  Map<String, String> extractToNewMap(String prefix, Properties properties) {
    def map = new HashMap<String, String>()
    PropertiesUtil.extractProperties(prefix, properties, map)
    return map
  }
}
