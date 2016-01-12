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

import com.google.common.io.Resources
import ratpack.server.internal.DefaultServerConfigBuilder
import ratpack.server.internal.ServerEnvironment
import ratpack.impose.Impositions
import spock.lang.Specification

import javax.net.ssl.SSLContext
import java.nio.file.Paths

class ServerConfigBuilderSystemPropertiesSpec extends Specification {

  ServerConfigBuilder builder
  def properties

  def setup() {
    properties = new Properties()
    builder = new DefaultServerConfigBuilder(new ServerEnvironment([:], properties), Impositions.none())
  }

  def "set port"() {
    given:
    properties.setProperty('ratpack.server.port', '5060')

    when:
    def config = builder.sysProps().build()

    then:
    config.port == 5060
  }

  def "set property from custom prefix"() {
    given:
    properties.setProperty('app.server.port', '6060')

    when:
    def config = builder.sysProps('app.').build()

    then:
    config.port == 6060
  }

  def "multiple sources override"() {
    given:
    properties.setProperty('ratpack.server.port', '5060')
    properties.setProperty('app.server.port', '8080')

    when:
    def config = builder.sysProps('app.').sysProps().build()

    then:
    config.port == 5060

    when:
    config = builder.sysProps().sysProps('app.').build()

    then:
    config.port == 8080
  }

  def "malformed port property uses default"() {
    given:
    properties.setProperty('ratpack.server.port', 'abcd')

    when:
    def config = builder.sysProps().build()

    then:
    config.port == ServerConfig.DEFAULT_PORT
  }

  def "set address"() {
    given:
    properties.setProperty('ratpack.server.address', 'localhost')

    when:
    def config = builder.sysProps().build()

    then:
    config.address.hostName == 'localhost'
  }

  def "malformed address property throws exception"() {
    given:
    properties.setProperty('ratpack.server.address', 'blah')

    when:
    builder.sysProps().build()

    then:
    thrown IllegalArgumentException
  }

  def "set development"() {
    given:
    properties.setProperty('ratpack.server.development', 'true')

    when:
    def config = builder.sysProps().build()

    then:
    config.development
  }

  def "non boolean development properties are false"() {
    given:
    properties.setProperty('ratpack.server.development', 'hi')

    when:
    def config = builder.sysProps().build()

    then:
    !config.development
  }

  def "set threads"() {
    given:
    properties.setProperty('ratpack.server.threads', '10')

    when:
    def config = builder.sysProps().build()

    then:
    config.threads == 10
  }

  def "malformed threads uses default"() {
    given:
    properties.setProperty('ratpack.server.threads', 'abcd')

    when:
    def config = builder.sysProps().build()

    then:
    config.threads == ServerConfig.DEFAULT_THREADS
  }

  def "set public address"() {
    given:
    properties.setProperty('ratpack.server.publicAddress', 'http://ratpack.io')

    when:
    def config = builder.sysProps().build()

    then:
    config.publicAddress.toString() == 'http://ratpack.io'
  }

  def "set max content length"() {
    given:
    properties.setProperty('ratpack.server.maxContentLength', '256')

    when:
    def config = builder.sysProps().build()

    then:
    config.maxContentLength == 256
  }

  def "malformed max content length uses default"() {
    given:
    properties.setProperty('ratpack.server.maxContentLength', 'abcd')

    when:
    def config = builder.sysProps().build()

    then:
    config.maxContentLength == ServerConfig.DEFAULT_MAX_CONTENT_LENGTH
  }

  def "set ssl context"() {
    given:
    String keystoreFile = Paths.get(Resources.getResource('ratpack/launch/internal/keystore.jks').toURI()).toString()
    String keystorePassword = 'password'
    properties.setProperty('ratpack.server.ssl.keystoreFile', keystoreFile)
    properties.setProperty('ratpack.server.ssl.keystorePassword', keystorePassword)

    when:
    SSLContext sslContext = builder.sysProps().build().sslContext

    then:
    sslContext
  }

  def "set connect timeout millis"() {
    given:
    properties.setProperty('ratpack.server.connectTimeoutMillis', '1000')

    when:
    def config = builder.sysProps().build()

    then:
    config.connectTimeoutMillis.get() == 1000
  }

  def "malformed connect timeout millis uses default"() {
    given:
    properties.setProperty('ratpack.server.connectTimeoutMillis', 'abcd')

    when:
    def config = builder.sysProps().build()

    then:
    !config.connectTimeoutMillis.present
  }

  def "set max messages per read"() {
    given:
    properties.setProperty('ratpack.server.maxMessagesPerRead', '10')

    when:
    def config = builder.sysProps().build()

    then:
    config.maxMessagesPerRead.get() == 10
  }

  def "malformed max messages per read uses default"() {
    given:
    properties.setProperty('ratpack.server.connectTimeoutMillis', 'abcd')

    when:
    def config = builder.sysProps().build()

    then:
    !config.maxMessagesPerRead.present
  }

  def "set receive buffer size"() {
    given:
    properties.setProperty('ratpack.server.receiveBufferSize', '1')

    when:
    def config = builder.sysProps().build()

    then:
    config.receiveBufferSize.get() == 1
  }

  def "malformed receive buffer size uses default"() {
    given:
    properties.setProperty('ratpack.server.receiveBufferSize', 'abcd')

    when:
    def config = builder.sysProps().build()

    then:
    !config.receiveBufferSize.present
  }

  def "set write spin count"() {
    given:
    properties.setProperty('ratpack.server.writeSpinCount', '100')

    when:
    def config = builder.sysProps().build()

    then:
    config.writeSpinCount.get() == 100
  }

  def "malformed write spin count uses default"() {
    given:
    properties.setProperty('ratpack.server.writeSpinCount', 'abcd')

    when:
    def config = builder.sysProps().build()

    then:
    !config.writeSpinCount.present
  }
}
