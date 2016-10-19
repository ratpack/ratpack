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

import ratpack.registry.Registry
import ratpack.test.ServerBackedApplicationUnderTest
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

class StartupErrorSpec extends Specification {

  def ex = new Exception("!")

  @AutoCleanup("stop")
  RatpackServer server

  TestHttpClient http = TestHttpClient.testHttpClient(ServerBackedApplicationUnderTest.of { server })

  def "errors in of function are fatal on startup"() {
    when:
    server = RatpackServer.of { throw ex }

    then:
    noExceptionThrown()

    when:
    server.start()

    then:
    def e = thrown(Exception)
    e.is ex
  }

  def "registry building errors are fatal when not in development"() {
    when:
    server = RatpackServer.of { it.serverConfig(ServerConfig.embedded().development(false)).registry { throw ex }.handlers { it.handler { it.render "ok" } } }

    then:
    noExceptionThrown()

    when:
    server.start()

    then:
    def e = thrown(StartupFailureException)
    e.cause.is ex
  }

  def "registry building errors are not fatal when in development"() {
    when:
    server = RatpackServer.of {
      it.serverConfig(ServerConfig.embedded().development(true))
        .registry { throw ex }
        .handlers {
        it.all { it.render "ok" }
      }
    }

    then:
    noExceptionThrown()

    when:
    server.start()

    then:
    noExceptionThrown()
    http.text.contains ex.message
  }

  def "registry building errors can be fixed when in development"() {
    when:
    def error = true
    server = RatpackServer.of {
      it.serverConfig(ServerConfig.embedded().development(true))
      it.registry {
        if (error) {
          throw ex
        } else {
          Registry.empty()
        }
      }
      it.handlers { it.all { it.render "ok" } }
    }

    then:
    noExceptionThrown()

    when:
    server.start()

    then:
    noExceptionThrown()
    http.text.contains ex.message

    when:
    error = false

    then:
    http.text == "ok"
  }

}
