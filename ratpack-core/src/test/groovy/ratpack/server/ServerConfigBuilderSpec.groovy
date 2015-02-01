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

  ServerConfig.Builder builder

  def setup() {
    builder = ServerConfig.noBaseDir()
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

  def "set compression min size"() {
    expect:
    builder.compressionMinSize(256L).build().compressionMinSize == 256
  }

  def "new builder has default compression white list"() {
    expect:
    builder.build().compressionMimeTypeWhiteList.size() == 0
  }

  def "set compression white list"() {
    when:
    Set<String> whiteList = builder.compressionWhiteListMimeTypes('json', 'xml').build().compressionMimeTypeWhiteList

    then:
    whiteList == ['json', 'xml'] as Set
  }

  def "new builder has default compression black list"() {
    expect:
    builder.build().compressionMimeTypeBlackList.size() == 0
  }

  def "set compression black list"() {
    when:
    Set<String> blackList = builder.compressionBlackListMimeTypes('json', 'xml').build().compressionMimeTypeBlackList

    then:
    blackList == ['json', 'xml'] as Set
  }

  def "new builder has default index files"() {
    expect:
    builder.build().indexFiles.size() == 0
  }

  def "set index files"() {
    when:
    Set<String> indexFiles = builder.indexFiles('home.html', 'index.html').build().indexFiles

    then:
    indexFiles == ['home.html', 'index.html'] as Set
  }

  def "set ssl context"() {
    given:
    SSLContext context = SSLContexts.sslContext(ServerConfigBuilderSpec.classLoader.getResourceAsStream('ratpack/launch/internal/keystore.jks'), 'password')

    when:
    SSLContext sslContext = builder.ssl(context).build().SSLContext

    then:
    sslContext

  }
}
