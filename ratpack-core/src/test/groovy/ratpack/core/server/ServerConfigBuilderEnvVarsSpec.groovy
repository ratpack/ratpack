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

package ratpack.core.server

import com.google.common.io.Resources
import io.netty.handler.ssl.SslContext
import io.netty.util.Mapping
import ratpack.core.impose.Impositions
import ratpack.core.server.internal.DefaultServerConfigBuilder
import ratpack.core.server.internal.ServerEnvironment
import ratpack.test.internal.BaseRatpackSpec

import java.nio.file.Paths

class ServerConfigBuilderEnvVarsSpec extends BaseRatpackSpec {

  private static ServerConfig build(Map<String, String> source, String env = ServerConfigBuilder.DEFAULT_ENV_PREFIX) {
    new DefaultServerConfigBuilder(new ServerEnvironment(source, new Properties()), Impositions.none()).env(env).build()
  }

  def "set port"() {
    expect:
    build('RATPACK_PORT': '5060').port == 5060
  }

  def "set property from custom prefix"() {
    expect:
    build('APP_SERVER__PORT': '6060', "APP_").port == 6060
  }

  def "multiple sources override"() {
    expect:
    build('RATPACK_SERVER__PORT': '5060', 'APP_SERVER__PORT': '8080').port == 5060
    build('RATPACK_SERVER__PORT': '5060', 'APP_SERVER__PORT': '8080', "APP_").port == 8080
  }

  def "malformed port property uses default"() {
    expect:
    build('RATPACK_PORT': 'abcd').port == ServerConfig.DEFAULT_PORT
  }

  def "set address"() {
    expect:
    build('RATPACK_SERVER__ADDRESS': 'localhost').address.hostName == "localhost"
  }

  def "malformed address property throws exception"() {
    when:
    build('RATPACK_SERVER__ADDRESS': 'blah').address.hostName == "localhost"

    then:
    thrown IllegalArgumentException
  }

  def "set development"() {
    expect:
    build('RATPACK_SERVER__DEVELOPMENT': 'true').development
  }

  def "non boolean development properties are false"() {
    expect:
    !build('RATPACK_DEVELOPMENT': 'hi').development
  }

  def "set threads"() {
    expect:
    build('RATPACK_SERVER__THREADS': '10').threads == 10
  }

  def "malformed threads uses default"() {
    expect:
    build('RATPACK_SERVER__THREADS': 'abcd').threads == ServerConfig.DEFAULT_THREADS
  }

  def "set public address"() {
    expect:
    build('RATPACK_SERVER__PUBLIC_ADDRESS': 'http://ratpack.io').publicAddress.toString() == "http://ratpack.io"
  }

  def "set max content length"() {
    expect:
    build('RATPACK_SERVER__MAX_CONTENT_LENGTH': '256').maxContentLength == 256
  }

  def "malformed max content length uses default"() {
    expect:
    build('RATPACK_SERVER__MAX_CONTENT_LENGTH': 'abcd').maxContentLength == ServerConfig.DEFAULT_MAX_CONTENT_LENGTH
  }

  def "set ssl context"() {
    given:
    String keystoreFile = Paths.get(Resources.getResource('ratpack/core/server/keystore.jks').toURI()).toString()
    String keystorePassword = 'password'

    expect:
    build('RATPACK_SERVER__SSL__KEYSTORE_FILE': keystoreFile, 'RATPACK_SERVER__SSL__KEYSTORE_PASSWORD': keystorePassword).sslContext
  }

  def "set sni ssl contexts"() {
    given:
    String keystoreFile = Paths.get(Resources.getResource('ratpack/core/server/keystore.jks').toURI()).toString()
    String keystorePassword = 'password'

    when:
    Mapping<String, SslContext> sslContext = build([
      'RATPACK_SERVER__SSL__KEYSTORE_FILE': keystoreFile,
      'RATPACK_SERVER__SSL__KEYSTORE_PASSWORD': keystorePassword,
      'RATPACK_SERVER__SSL__RATPACK_IO__KEYSTORE_FILE': keystoreFile,
      'RATPACK_SERVER__SSL__RATPACK_IO__KEYSTORE_PASSWORD': keystorePassword,
      'RATPACK_SERVER__SSL___EXAMPLE_COM__KEYSTORE_FILE': keystoreFile,
      'RATPACK_SERVER__SSL___EXAMPLE_COM__KEYSTORE_PASSWORD': keystorePassword
    ]).sslContext

    then:
    sslContext.defaultValue
    sslContext.map.size() == 2

    def defaultValue = sslContext.map("example.com")
    def exampleValue = sslContext.map("api.example.com")
    def ratpackValue = sslContext.map("ratpack.io")

    defaultValue != exampleValue
    defaultValue != ratpackValue
    exampleValue != ratpackValue

  }

  def "set connect timeout millis"() {
    expect:
    build('RATPACK_SERVER__CONNECT_TIMEOUT_MILLIS': '1000').connectTimeoutMillis.get() == 1000
  }

  def "malformed connect timeout millis uses default"() {
    expect:
    !build('RATPACK_SERVER__CONNECT_TIMEOUT_MILLIS': 'abcd').connectTimeoutMillis.present
  }

  def "set max messages per read"() {
    expect:
    build('RATPACK_SERVER__MAX_MESSAGES_PER_READ': '1').maxMessagesPerRead.get() == 1
  }

  def "malformed max messages per read uses default"() {
    expect:
    !build('RATPACK_SERVER__MAX_MESSAGES_PER_READ': 'abcd').maxMessagesPerRead.present
  }

  def "set receive buffer size"() {
    expect:
    build('RATPACK_SERVER__RECEIVE_BUFFER_SIZE': '1').receiveBufferSize.get() == 1
  }

  def "malformed receive buffer size uses default"() {
    expect:
    !build('RATPACK_SERVER__RECEIVE_BUFFER_SIZE': 'abcd').receiveBufferSize.present
  }

  def "set write spin count"() {
    expect:
    build('RATPACK_SERVER__WRITE_SPIN_COUNT': '1').writeSpinCount.get() == 1
  }

  def "malformed write spin count uses default"() {
    expect:
    !build('RATPACK_SERVER__WRITE_SPIN_COUNT': 'abcd').writeSpinCount.present
  }

}
