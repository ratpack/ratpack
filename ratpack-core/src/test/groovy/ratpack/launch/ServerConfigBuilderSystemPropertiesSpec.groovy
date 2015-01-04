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

package ratpack.launch

import spock.lang.Specification

class ServerConfigBuilderSystemPropertiesSpec extends Specification {

  ServerConfigBuilder builder

  def setup() {
    builder = ServerConfigBuilder.noBaseDir()
  }

  def "set port"() {
    given:
    System.setProperty('ratpack.port', '5060')

    when:
    def config = builder.sysProps().build()

    then:
    config.port == 5060

    cleanup:
    System.clearProperty('ratpack.port')
  }

  def "set property from custom prefix"() {
    given:
    System.setProperty('app.port', '6060')

    when:
    def config = builder.sysProps('app.').build()

    then:
    config.port == 6060

    cleanup:
    System.clearProperty('app.port')
  }

  def "multiple sources override"() {
    given:
    System.setProperty('ratpack.port', '5060')
    System.setProperty('app.port', '8080')

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
    System.setProperty('ratpack.port', 'abcd')

    when:
    builder.sysProps()

    then:
    thrown NumberFormatException

    cleanup:
    System.clearProperty('ratpack.port')
  }

  def "set address"() {
    given:
    System.setProperty('ratpack.address', 'localhost')

    when:
    def config = builder.sysProps().build()

    then:
    config.address.hostName == 'localhost'

    cleanup:
    System.clearProperty('ratpack.address')
  }

  def "malformed address property throws exception"() {
    given:
    System.setProperty('ratpack.address', 'blah')

    when:
    builder.sysProps()

    then:
    thrown RuntimeException

    cleanup:
    System.clearProperty('ratpack.address')
  }

  def "set development"() {
    given:
    System.setProperty('ratpack.development', 'true')

    when:
    def config = builder.sysProps().build()

    then:
    config.development

    cleanup:
    System.clearProperty('ratpack.development')
  }

  def "non boolean development properties are false"() {
    given:
    System.setProperty('ratpack.development', 'hi')

    when:
    def config = builder.sysProps().build()

    then:
    !config.development

    cleanup:
    System.clearProperty('ratpack.development')
  }

  def "set threads"() {
    given:
    System.setProperty('ratpack.threads', '10')

    when:
    def config = builder.sysProps().build()

    then:
    config.threads == 10

    cleanup:
    System.clearProperty('ratpack.threads')
  }

  def "malformed threads throws exception"() {
    given:
    System.setProperty('ratpack.threads', 'abcd')

    when:
    builder.sysProps()

    then:
    thrown NumberFormatException

    cleanup:
    System.clearProperty('ratpack.threads')
  }

  def "set public address"() {
    given:
    System.setProperty('ratpack.publicAddress', 'http://ratpack.io')

    when:
    def config = builder.sysProps().build()

    then:
    config.publicAddress.toString() == 'http://ratpack.io'

    cleanup:
    System.clearProperty('ratpack.publicAddress')
  }

  def "set max content length"() {
    given:
    System.setProperty('ratpack.maxContentLength', '256')

    when:
    def config = builder.sysProps().build()

    then:
    config.maxContentLength == 256

    cleanup:
    System.clearProperty('ratpack.maxContentLength')
  }

  def "malformed max content length throws exception"() {
    given:
    System.setProperty('ratpack.maxContentLength', 'abcd')

    when:
    builder.sysProps()

    then:
    thrown NumberFormatException

    cleanup:
    System.clearProperty('ratpack.maxContentLength')
  }

  def "set time responses"() {
    given:
    System.setProperty('ratpack.timeResponses', 'true')

    when:
    def config = builder.sysProps().build()

    then:
    config.timeResponses

    cleanup:
    System.clearProperty('ratpack.timeResponses')
  }

  def "none boolean time responses are false"() {
    given:
    System.setProperty('ratpack.timeResponses', 'abcd')

    when:
    def config = builder.sysProps().build()

    then:
    !config.timeResponses

    cleanup:
    System.clearProperty('ratpack.timeResponses')
  }

  def "set compress responses"() {
    given:
    System.setProperty('ratpack.compressResponses', 'true')

    when:
    def config = builder.sysProps().build()

    then:
    config.compressResponses

    cleanup:
    System.clearProperty('ratpack.compressResponses')
  }

  def "none boolean compressResponses responses are false"() {
    given:
    System.setProperty('ratpack.compressResponses', 'abcd')

    when:
    def config = builder.sysProps().build()

    then:
    !config.compressResponses

    cleanup:
    System.clearProperty('ratpack.compressResponses')
  }

  def "set compression min size"() {
    given:
    System.setProperty('ratpack.compressionMinSize', '256')

    when:
    def config = builder.sysProps().build()

    then:
    config.compressionMinSize == 256L

    cleanup:
    System.clearProperty('ratpack.compressionMinSize')
  }

  def "malformed compress min size throws exception"() {
    given:
    System.setProperty('ratpack.compressionMinSize', 'abcd')

    when:
    builder.sysProps()

    then:
    thrown NumberFormatException

    cleanup:
    System.clearProperty('ratpack.compressionMinSize')
  }

  def "set compression white list"() {
    given:
    System.setProperty('ratpack.compressionWhiteListMimeTypes', 'json,xml')

    when:
    def config = builder.sysProps().build()

    then:
    config.compressionMimeTypeWhiteList == ['json', 'xml'] as Set

    cleanup:
    System.clearProperty('ratpack.compressionWhiteListMimeTypes')
  }

  def "set compression black list"() {
    given:
    System.setProperty('ratpack.compressionBlackListMimeTypes', 'json,xml')

    when:
    def config = builder.sysProps().build()

    then:
    config.compressionMimeTypeBlackList == ['json', 'xml'] as Set

    cleanup:
    System.clearProperty('ratpack.compressionBlackListMimeTypes')
  }

  def "set index files"() {
    given:
    System.setProperty('ratpack.indexFiles', 'home.html,index.html')

    when:
    def config = builder.sysProps().build()

    then:
    config.indexFiles == ['home.html', 'index.html']

    cleanup:
    System.clearProperty('ratpack.indexFiles')
  }

  def "trim white space in comma separated lists"() {
    given:
    System.setProperty('ratpack.indexFiles', 'home.html , index.html')

    when:
    def config = builder.sysProps().build()

    then:
    config.indexFiles == ['home.html', 'index.html']

    cleanup:
    System.clearProperty('ratpack.indexFiles')
  }
}
