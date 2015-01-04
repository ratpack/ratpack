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

package ratpack.launch

import ratpack.handling.Handler
import ratpack.registry.Registry
import spock.lang.Specification

class ServerConfigBuilderSpec extends Specification {

  ServerConfigBuilder builder

  def setup() {
    builder = ServerConfigBuilder.noBaseDir()
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
    def server = RatpackLauncher.with(config)
      .build(new HandlerFactory() {
        @Override
        Handler create(Registry rootRegistry) throws Exception {
          throw e
        }
      })

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

  def "new builder has default time responses"() {
    expect:
    !builder.build().timeResponses
  }

  def "set time responses"() {
    expect:
    builder.timeResponses(true).build().timeResponses
  }

  def "new builder has default compress responses"() {
    expect:
    !builder.build().compressResponses
  }

  def "set compress responses"() {
    expect:
    builder.compressResponses(true).build().compressResponses
  }

  def "new builder has default compression min size"() {
    expect:
    builder.build().compressionMinSize == ServerConfig.DEFAULT_COMPRESSION_MIN_SIZE
  }

  def "set compressiong min size"() {
    expect:
    builder.compressionMinSize(256L).build().compressionMinSize == 256
  }
}
