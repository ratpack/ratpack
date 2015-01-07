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

import ratpack.handling.Handler
import ratpack.test.ApplicationUnderTest
import spock.lang.Specification

class RatpackServerSpec extends Specification {

  RatpackServer server

  def cleanup() {
    if (server.isRunning()) {
      server.stop()
    }
  }

  def "start default server"() {
    given:
    server = RatpackServer.of { spec ->
      spec
        .handler {
        return {} as Handler
      }
    }

    when:
    server.start()

    then:
    server.running
    server.bindPort == 5050
  }

  def "start server on port"() {
    given:
    server = RatpackServer.of { spec ->
      spec
        .config(ServerConfig.noBaseDir().port(5060))
        .handler {
        return {} as Handler
      }
    }

    when:
    server.start()

    then:
    server.running
    server.bindPort == 5060
  }

  def "registry changes are applied on reload"() {
    given:
    def value = "foo"
    server = RatpackServer.of {
      it
        .registryOf { it.add(String, value) }
        .handler { return { it.render it.get(String) } as Handler }
    }
    def client = ApplicationUnderTest.of(server).httpClient

    when:
    server.start()

    then:
    server.isRunning()
    client.text == "foo"

    when:
    value = "bar"
    server.reload()

    then:
    server.isRunning()
    client.text == "bar"
  }

  def "configuration changes are applied up on reload"() {
    given:
    def config = ServerConfig.noBaseDir().build()

    server = RatpackServer.of {
      it
        .config(config)
        .handler { return {} as Handler }
    }

    when:
    server.start()

    then:
    server.running
    server.bindPort == 5050

    when:
    config = ServerConfig.noBaseDir().port(6060).build()
    server.reload()

    then:
    server.running
    server.bindPort == 6060
  }

  def "handler changes are applied on reload"() {
    def handler = {
      it.render 'foo'
    } as Handler

    server = RatpackServer.of {
      it
        .handler { return handler }
    }
    def client = ApplicationUnderTest.of(server).httpClient

    when:
    server.start()

    then:
    server.running
    client.text == 'foo'

    when:
    handler = {
      it.render 'bar'
    } as Handler

    server.reload()

    then:
    server.running
    client.text == 'bar'
  }
}
