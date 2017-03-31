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

package ratpack.server.internal

import spock.lang.Specification

class ServerEnvironmentSpec extends Specification {

  ServerEnvironment env(Map<String, String> env, Map<String, String> props) {
    def p = new Properties()
    p.putAll(props)
    new ServerEnvironment(env, p)
  }

  def "address"() {
    expect:
    env([:], [:]).address == null
    env([RATPACK_ADDRESS: "192.168.1.2"], [:]).address == InetAddress.getByName('192.168.1.2')
    env([:], ["ratpack.address": "192.168.1.2"]).address == InetAddress.getByName('192.168.1.2')
    env([RATPACK_ADDRESS: "192.168.1.3"], ["ratpack.address": "192.168.1.2"]).address == InetAddress.getByName('192.168.1.2')
    env([:], ["ratpack.address": "192.168.1.2"]).address == InetAddress.getByName('192.168.1.2')
  }

  def "port"() {
    expect:
    env([:], [:]).port == 5050
    env([PORT: "2020"], [:]).port == 2020
    env([RATPACK_PORT: "2020"], [:]).port == 2020
    env([:], ["ratpack.port": "2020"]).port == 2020
    env([PORT: "2020", RATPACK_PORT: "3030"], [:]).port == 3030
    env([PORT: "2020"], ["ratpack.port": "3030"]).port == 3030
    env([RATPACK_PORT: "2020"], ["ratpack.port": "3030"]).port == 3030
    env([PORT: "-1"], [:]).port == 5050
    env([RATPACK_PORT: "-1"], [:]).port == 5050
    env([:], ["ratpack.port": "-1"]).port == 5050
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

  def "publicAddress"() {
    expect:
    !env([:], [:]).publicAddress
    env([RATPACK_PUBLIC_ADDRESS: "http://example.com:2020"], [:]).publicAddress == URI.create("http://example.com:2020")
    env([:], ["ratpack.publicAddress":"http://example.com:2020"]).publicAddress == URI.create("http://example.com:2020")
    env([RATPACK_PUBLIC_ADDRESS: "http://example.com:2020"], ["ratpack.publicAddress":"http://example.com:3030"]).publicAddress == URI.create("http://example.com:3030")
    !env([RATPACK_PUBLIC_ADDRESS: "bad://example.com:2020"], ["ratpack.publicAddress":"bad://example.com:3030"]).publicAddress
    env([RATPACK_PUBLIC_ADDRESS: "bad://example.com:2020"], ["ratpack.publicAddress":"http://example.com:3030"]).publicAddress == URI.create("http://example.com:3030")
    env([RATPACK_PUBLIC_ADDRESS: "http://example.com:2020"], ["ratpack.publicAddress":"bad://example.com:3030"]).publicAddress == URI.create("http://example.com:2020")
    env([RATPACK_PUBLIC_ADDRESS: "example.com:2020"], [:]).publicAddress == URI.create("http://example.com:2020")
    env([RATPACK_PUBLIC_ADDRESS: "example.com"], [:]).publicAddress == URI.create("http://example.com")
    env([RATPACK_PUBLIC_ADDRESS: "192.168.1.151:2020"], [:]).publicAddress == URI.create("http://192.168.1.151:2020")
    env([RATPACK_PUBLIC_ADDRESS: "192.168.1.151"], [:]).publicAddress == URI.create("http://192.168.1.151")
    !env([RATPACK_PUBLIC_ADDRESS: "bad://192.168.1.151"], [:]).publicAddress
  }

  def "is implicitly development when started from intellij but not when running tests"() {
    expect:
    env([:], [(ServerEnvironment.SUN_JAVA_COMMAND): "$ServerEnvironment.INTELLIJ_MAIN RealMainClass".toString()]).development
    !env([:], [(ServerEnvironment.SUN_JAVA_COMMAND): "$ServerEnvironment.INTELLIJ_MAIN $ServerEnvironment.INTELLIJ_JUNIT".toString()]).development
  }

}
