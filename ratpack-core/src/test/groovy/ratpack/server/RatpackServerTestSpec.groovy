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

import ratpack.func.Action
import ratpack.handling.Handler
import ratpack.test.ApplicationUnderTest
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

class RatpackServerTestSpec extends Specification {

  @AutoCleanup("stop")
  RatpackServer server
  def http = TestHttpClient.testHttpClient(ApplicationUnderTest.of({ server }))

  def cleanup() {
    if (server.isRunning()) {
      server.stop()
    }
  }

  def "start default server"() {
    given:
    server = RatpackServer.of { spec ->
      spec
        .serverConfig(ServerConfig.embedded())
        .handler {
        return {} as Handler
      }
    }

    when:
    server.start()

    then:
    server.running
  }

  def "registry changes are applied on reload"() {
    given:
    def value = "foo"
    server = RatpackServer.of {
      it
        .serverConfig(ServerConfig.embedded())
        .registryOf { it.add(String, value) }
        .handler { return { it.render it.get(String) } as Handler }
    }

    when:
    server.start()

    then:
    server.isRunning()
    http.text == "foo"

    when:
    value = "bar"
    server.reload()

    then:
    server.isRunning()
    http.text == "bar"
  }

  def "configuration changes are applied up on reload"() {
    given:
    def config = ServerConfig.embedded().development(true).build()

    server = RatpackServer.of {
      it
        .serverConfig(config)
        .handler { return { it.render(it.serverConfig.development.toString()) } as Handler }
    }

    when:
    server.start()

    then:
    server.running
    http.text == "true"

    when:
    config = ServerConfig.embedded().development(false).build()
    server.reload()

    then:
    server.running
    http.text == "false"
  }

  def "handler changes are applied on reload"() {
    given:
    def handler = {
      it.render 'foo'
    } as Handler

    server = RatpackServer.of {
      it.serverConfig(ServerConfig.embedded())
      it.handler { return handler }
    }

    when:
    server.start()

    then:
    server.running
    http.text == 'foo'

    when:
    handler = {
      it.render 'bar'
    } as Handler

    server.reload()

    then:
    server.running
    http.text == 'bar'
  }

  def "server lifecycle events are executed with event data"() {
    given:
    def counter = 0
    def reloadCounter = 0

    server = RatpackServer.of {
      it.serverConfig(ServerConfig.embedded().development(false))
      it.registryOf {
        it.add(Integer, 5)
        it.add(Service, new Service() {

          @Override
          void onStart(StartEvent event) throws Exception {
            if (!event.reload) {
              counter++
            }
          }

          @Override
          void onStop(StopEvent event) throws Exception {
            if (!event.reload) {
              counter += event.registry.get(Integer)
            }
          }
        })
        it.add(Service, new Service() {

          @Override
          void onStart(StartEvent event) throws Exception {
            if (event.reload) {
              reloadCounter++
            }
          }

          @Override
          void onStop(StopEvent event) throws Exception {
            if (event.reload) {
              reloadCounter++
            }
          }
        })
      }
      it.handler { return {} as Handler }
    }

    when:
    server.start()

    then:
    server.running
    counter == 1
    reloadCounter == 0

    when:
    server.reload()

    then:
    server.running
    counter == 1
    reloadCounter == 2

    when:
    server.stop()

    then:
    !server.running
    counter == 6
    reloadCounter == 2

  }

  def "netty configuration is applied"() {
    given:
    server = RatpackServer.of ({
      it
        .serverConfig(ServerConfig.embedded()
          .connectTimeoutMillis(1000)
          .maxMessagesPerRead(3)
          .writeSpinCount(10))
        .handlers {
          it.path("connectTimeoutMillis") { it.render it.directChannelAccess.channel.config().connectTimeoutMillis.toString() }
          it.path("maxMessagesPerRead") { it.render it.directChannelAccess.channel.config().maxMessagesPerRead.toString() }
          it.path("writeSpinCount") { it.render it.directChannelAccess.channel.config().writeSpinCount.toString() }
        }
    } as Action<RatpackServerSpec>)

    when:
    server.start()

    then:
    http.getText("connectTimeoutMillis") == "1000"
    http.getText("maxMessagesPerRead") == "3"
    http.getText("writeSpinCount") == "10"
  }
}
