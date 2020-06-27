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

package ratpack.func

import spock.lang.Specification

class UtilsSpec extends Specification {

  def cleanup() {
    Utils.env = System.getenv()
    Utils.properties = System.getProperties()
  }

  def "development"() {
    given:
    Utils.env = env
    Utils.properties = props as Properties

    expect:
    Utils.isDevelopment() == development

    where:
    env                            | props                            || development
    [:]                            | [:]                              || false
    [RATPACK_DEVELOPMENT: "true"]  | [:]                              || true
    [:]                            | ["ratpack.development": "true"]  || true
    [RATPACK_DEVELOPMENT: "true"]  | ["ratpack.development": "false"] || false
    [RATPACK_DEVELOPMENT: "false"] | ["ratpack.development": "true"]  || true
    [RATPACK_DEVELOPMENT: "-1"]    | [:]                              || false
    [:]                            | ["ratpack.development": "-1"]    || false
  }

  def "is implicitly development when started from intellij but not when running tests"() {
    given:
    Utils.env = env
    Utils.properties = props as Properties

    expect:
    Utils.isDevelopment() == development

    where:
    env | props                                                                                                 || development
    [:] | [(Utils.SUN_JAVA_COMMAND): "$Utils.INTELLIJ_MAIN RealMainClass".toString()]               || true
    [:] | [(Utils.SUN_JAVA_COMMAND): "$Utils.INTELLIJ_MAIN $Utils.INTELLIJ_JUNIT".toString()] || false
  }
}
