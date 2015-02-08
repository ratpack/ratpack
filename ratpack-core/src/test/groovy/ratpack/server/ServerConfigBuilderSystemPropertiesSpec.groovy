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

import ratpack.server.internal.DefaultServerConfigBuilder
import spock.lang.Specification

import javax.net.ssl.SSLContext

class ServerConfigBuilderSystemPropertiesSpec extends Specification {

  ServerConfig.Builder builder
  def properties

  def setup() {
    properties = new Properties()
    builder = DefaultServerConfigBuilder.noBaseDir(new ServerEnvironment([:], properties))
  }

  def "set port"() {
    given:
    properties.setProperty('ratpack.port', '5060')

    when:
    def config = builder.sysProps().build()

    then:
    config.port == 5060
  }

  def "set property from custom prefix"() {
    given:
    properties.setProperty('app.port', '6060')

    when:
    def config = builder.sysProps('app.').build()

    then:
    config.port == 6060
  }

  def "multiple sources override"() {
    given:
    properties.setProperty('ratpack.port', '5060')
    properties.setProperty('app.port', '8080')

    when:
    def config = builder.sysProps('app.').sysProps().build()

    then:
    config.port == 5060

    when:
    config = builder.sysProps().sysProps('app.').build()

    then:
    config.port == 8080
  }

  def "malformed port property throws exception"() {
    given:
    properties.setProperty('ratpack.port', 'abcd')

    when:
    builder.sysProps()

    then:
    thrown NumberFormatException
  }

  def "set address"() {
    given:
    properties.setProperty('ratpack.address', 'localhost')

    when:
    def config = builder.sysProps().build()

    then:
    config.address.hostName == 'localhost'
  }

  def "malformed address property throws exception"() {
    given:
    properties.setProperty('ratpack.address', 'blah')

    when:
    builder.sysProps()

    then:
    thrown RuntimeException
  }

  def "set development"() {
    given:
    properties.setProperty('ratpack.development', 'true')

    when:
    def config = builder.sysProps().build()

    then:
    config.development
  }

  def "non boolean development properties are false"() {
    given:
    properties.setProperty('ratpack.development', 'hi')

    when:
    def config = builder.sysProps().build()

    then:
    !config.development
  }

  def "set threads"() {
    given:
    properties.setProperty('ratpack.threads', '10')

    when:
    def config = builder.sysProps().build()

    then:
    config.threads == 10
  }

  def "malformed threads throws exception"() {
    given:
    properties.setProperty('ratpack.threads', 'abcd')

    when:
    builder.sysProps()

    then:
    thrown NumberFormatException
  }

  def "set public address"() {
    given:
    properties.setProperty('ratpack.publicAddress', 'http://ratpack.io')

    when:
    def config = builder.sysProps().build()

    then:
    config.publicAddress.toString() == 'http://ratpack.io'
  }

  def "set max content length"() {
    given:
    properties.setProperty('ratpack.maxContentLength', '256')

    when:
    def config = builder.sysProps().build()

    then:
    config.maxContentLength == 256
  }

  def "malformed max content length throws exception"() {
    given:
    properties.setProperty('ratpack.maxContentLength', 'abcd')

    when:
    builder.sysProps()

    then:
    thrown NumberFormatException
  }

  def "set ssl context"() {
    given:
    String keystoreFile = 'ratpack/launch/internal/keystore.jks'
    String keystorePassword = 'password'
    properties.setProperty('ratpack.sslKeystoreFile', keystoreFile)
    properties.setProperty('ratpack.sslKeystorePassword', keystorePassword)

    when:
    SSLContext sslContext = builder.sysProps().build().SSLContext

    then:
    sslContext
  }
}
