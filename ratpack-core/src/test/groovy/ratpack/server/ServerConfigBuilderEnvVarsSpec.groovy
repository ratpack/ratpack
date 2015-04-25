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
import spock.lang.Specification

import javax.net.ssl.SSLContext
import java.nio.file.Paths

class ServerConfigBuilderEnvVarsSpec extends Specification {

  ServerConfig.Builder builder
  Map<String, String> source

  def setup() {
    source = [:]
    builder = DefaultServerConfigBuilder.noBaseDir(new ServerEnvironment(source, new Properties()))
  }

  def "set port"() {
    given:
    source['RATPACK_PORT'] = '5060'

    when:
    def config = builder.env().build()

    then:
    config.port == 5060
  }

  def "set property from custom prefix"() {
    given:
    source['APP_SERVER__PORT'] = '6060'

    when:
    def config = builder.env('APP_').build()

    then:
    config.port == 6060
  }

  def "multiple sourcees override"() {
    given:
    source['RATPACK_SERVER__PORT'] = '5060'
    source['APP_SERVER__PORT'] = '8080'

    when:
    def config = builder.env('APP_').env().build()

    then:
    config.port == 5060

    when:
    config = builder.env().env('APP_').build()

    then:
    config.port == 8080
  }

  def "malformed port property uses default"() {
    given:
    source['RATPACK_PORT'] = 'abcd'

    when:
    def config = builder.env().build()

    then:
    config.port == ServerConfig.DEFAULT_PORT
  }

  def "set address"() {
    given:
    source['RATPACK_SERVER__ADDRESS'] = 'localhost'

    when:
    def config = builder.env().build()

    then:
    config.address.hostName == 'localhost'
  }

  def "malformed address property throws exception"() {
    given:
    source['RATPACK_SERVER__ADDRESS'] = 'blah'

    when:
    builder.env().build()

    then:
    thrown IllegalArgumentException
  }

  def "set development"() {
    given:
    source['RATPACK_SERVER__DEVELOPMENT'] = 'true'

    when:
    def config = builder.env().build()

    then:
    config.development
  }

  def "non boolean development properties are false"() {
    given:
    source['RATPACK_DEVELOPMENT'] = 'hi'

    when:
    def config = builder.env().build()

    then:
    !config.development
  }

  def "set threads"() {
    given:
    source['RATPACK_SERVER__THREADS'] = '10'

    when:
    def config = builder.env().build()

    then:
    config.threads == 10
  }

  def "malformed threads uses default"() {
    given:
    source['RATPACK_SERVER__THREADS'] = 'abcd'

    when:
    def config = builder.env().build()

    then:
    config.threads == ServerConfig.DEFAULT_THREADS
  }

  def "set public address"() {
    given:
    source['RATPACK_SERVER__PUBLIC_ADDRESS'] = 'http://ratpack.io'

    when:
    def config = builder.env().build()

    then:
    config.publicAddress.toString() == 'http://ratpack.io'
  }

  def "set max content length"() {
    given:
    source['RATPACK_SERVER__MAX_CONTENT_LENGTH'] = '256'

    when:
    def config = builder.env().build()

    then:
    config.maxContentLength == 256
  }

  def "malformed max content length uses default"() {
    given:
    source['RATPACK_SERVER__MAX_CONTENT_LENGTH'] = 'abcd'

    when:
    def config = builder.env().build()

    then:
    config.maxContentLength == ServerConfig.DEFAULT_MAX_CONTENT_LENGTH
  }

  def "set ssl context"() {
    given:
    String keystoreFile = Paths.get(Resources.getResource('ratpack/launch/internal/keystore.jks').toURI()).toString()
    String keystorePassword = 'password'
    source['RATPACK_SERVER__SSL__KEYSTORE_FILE'] = keystoreFile
    source['RATPACK_SERVER__SSL__KEYSTORE_PASSWORD'] = keystorePassword

    when:
    SSLContext sslContext = builder.env().build().SSLContext

    then:
    sslContext
  }

}
