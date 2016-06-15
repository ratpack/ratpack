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

package ratpack.server

import ratpack.ssl.SSLContexts
import spock.lang.Specification

import javax.net.ssl.SSLContext

class ServerConfigBuilderSpec extends Specification {

  ServerConfigBuilder builder

  def setup() {
    builder = ServerConfig.builder()
  }

  def "no base dir"() {
    given:
    ServerConfig serverConfig = builder.build()

    when:
    serverConfig.baseDir

    then:
    thrown(NoBaseDirException)
  }

  def "error subclass thrown from HandlerFactory's create method"() {
    given:
    def e = new Error("e")
    def config = builder.build()
    def server = RatpackServer.of { spec -> spec.serverConfig(config).handler { throw e } }

    when:
    server.start()

    then:
    thrown(Error)
    !server.running

    cleanup:
    if (server && server.running) {
      server.stop()
    }
  }

  def "new builder has default port"() {
    expect:
    builder.build().port == ServerConfig.DEFAULT_PORT
  }

  def "set port"() {
    expect:
    builder.port(5060).build().port == 5060
  }

  def "new builder has default address"() {
    expect:
    builder.build().address == null
  }

  def "set address"() {
    given:
    InetAddress address = InetAddress.getByName('localhost')

    when:
    def config = builder.address(address).build()

    then:
    config.address == address
  }

  def "new builder has default development setting"() {
    expect:
    !builder.build().development
  }

  def "set development"() {
    expect:
    builder.development(true).build().development
  }

  def "new builder has default thread count"() {
    expect:
    builder.build().threads == ServerConfig.DEFAULT_THREADS
  }

  def "set threads"() {
    expect:
    builder.threads(10).build().threads == 10
  }

  def "minimum of 1 thread"() {
    when:
    builder.threads(0)

    then:
    thrown IllegalArgumentException
  }

  def "new builder has default public address"() {
    expect:
    builder.build().publicAddress == null
  }

  def "set public address"() {
    expect:
    builder.publicAddress(URI.create('http://ratpack.io')).build().publicAddress.toString() == 'http://ratpack.io'
  }

  def "new builder has default max content length"() {
    expect:
    builder.build().maxContentLength == ServerConfig.DEFAULT_MAX_CONTENT_LENGTH
  }

  def "set max content length"() {
    expect:
    builder.maxContentLength(256).build().maxContentLength == 256
  }

  def "new builder has default max chunk size"() {
    expect:
    builder.build().maxChunkSize == ServerConfig.DEFAULT_MAX_CHUNK_SIZE
  }

  def "set max chunk size"() {
    expect:
    builder.maxChunkSize(256).build().maxChunkSize == 256
  }

  def "new builder has default max initial line length"() {
    expect:
    builder.build().maxInitialLineLength == ServerConfig.DEFAULT_MAX_INITIAL_LINE_LENGTH
  }

  def "set max initial line length"() {
    expect:
    builder.maxInitialLineLength(256).build().maxInitialLineLength == 256
  }

  def "new builder has default max header size"() {
    expect:
    builder.build().maxHeaderSize == ServerConfig.DEFAULT_MAX_HEADER_SIZE
  }

  def "set max header size"() {
    expect:
    builder.maxHeaderSize(256).build().maxHeaderSize == 256
  }

  def "set ssl context"() {
    given:
    SSLContext context = SSLContexts.sslContext(ServerConfigBuilderSpec.classLoader.getResourceAsStream('ratpack/launch/internal/keystore.jks'), 'password')

    when:
    SSLContext sslContext = builder.ssl(context).build().sslContext

    then:
    sslContext

  }

  def "new builder has default connect timeout millis"() {
    expect:
    !builder.build().connectTimeoutMillis.present
  }

  def "set connect timeout millis"() {
    expect:
    builder.connectTimeoutMillis(1000).build().connectTimeoutMillis.get() == 1000
  }

  def "new builder has default max messages per read"() {
    expect:
    !builder.build().maxMessagesPerRead.present
  }

  def "set max messages per read"() {
    expect:
    builder.maxMessagesPerRead(5).build().maxMessagesPerRead.get() == 5
  }

  def "new builder has default receive buffers size"() {
    expect:
    !builder.build().receiveBufferSize.present
  }

  def "set receive buffers size"() {
    expect:
    builder.receiveBufferSize(256).build().receiveBufferSize.get() == 256
  }

  def "new builder has default write spin count"() {
    expect:
    !builder.build().writeSpinCount.present
  }

  def "set write spin count"() {
    expect:
    builder.writeSpinCount(2).build().writeSpinCount.get() == 2
  }
}
