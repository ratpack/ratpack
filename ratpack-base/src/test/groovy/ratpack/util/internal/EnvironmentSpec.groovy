/*
 * Copyright 2017 the original author or authors.
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

class EnvironmentSpec extends Specification {

  Environment env(Map<String, String> env, Map<String, String> props) {
    def p = new Properties()
    p.putAll(props)
    new Environment(env, p)
  }

  def "development"() {
    expect:
    !env([:], [:]).development
    env([RATPACK_DEVELOPMENT: "true"], [:]).development
    env([:], ["ratpack.development": "true"]).development
    !env([RATPACK_DEVELOPMENT: "true"], ["ratpack.development": "false"]).development
    env([RATPACK_DEVELOPMENT: "false"], ["ratpack.development": "true"]).development
    !env([RATPACK_DEVELOPMENT: "-1"], [:]).development
    !env([:], ["ratpack.development": "-1"]).development
  }

  def "is implicitly development when started from intellij but not when running tests"() {
    expect:
    env([:], [(Environment.SUN_JAVA_COMMAND): "$Environment.INTELLIJ_MAIN RealMainClass".toString()]).development
    !env([:], [(Environment.SUN_JAVA_COMMAND): "$Environment.INTELLIJ_MAIN $Environment.INTELLIJ_JUNIT".toString()]).development
  }

}
