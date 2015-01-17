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

package ratpack.server

import spock.lang.Specification

class ServerEnvironmentTest extends Specification {

  ServerEnvironment env(Map<String, String> env, Map<String, String> props) {
    def p = new Properties()
    p.putAll(props)
    new ServerEnvironment(env, p)
  }

  def "port"() {
    expect:
    env([:], [:]).port == 5050
    env([PORT: "2020"], [:]).port == 2020
    env([PORT: "2020"], ["ratpack.port": "3030"]).port == 3030
    env([PORT: "2020", RATPACK_PORT: "3030"], [:]).port == 3030
    env([PORT: "-1"], [:]).port == 5050
  }
}
