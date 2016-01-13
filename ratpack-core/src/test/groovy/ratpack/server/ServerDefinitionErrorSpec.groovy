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

import ratpack.handling.Handler
import ratpack.registry.Registry
import ratpack.server.internal.ServerEnvironment
import ratpack.test.ServerBackedApplicationUnderTest
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

class ServerDefinitionErrorSpec extends Specification {

  @AutoCleanup("stop")
  RatpackServer server

  @Delegate
  TestHttpClient httpClient = TestHttpClient.testHttpClient(ServerBackedApplicationUnderTest.of { server })

  def setup() {
    implicitlyDevelopment(true)
    System.setProperty(ServerEnvironment.PORT_PROPERTY, "0")
  }

  static implicitlyDevelopment(boolean flag) {
    System.setProperty(ServerEnvironment.DEVELOPMENT_PROPERTY, flag.toString())
  }

  def cleanup() {
    System.clearProperty(ServerEnvironment.DEVELOPMENT_PROPERTY)
    System.clearProperty(ServerEnvironment.PORT_PROPERTY)
  }

  def "can fix initial startup error in of function in implicit development"() {
    given:
    def error = "bang"

    when:
    server = RatpackServer.of {
      if (error) {
        throw new Exception(error)
      } else {
        it.handler {
          return { it.render("ok") } as Handler
        }
      }
    }

    then:
    getText().contains "bang"

    when:
    error = "boom"

    then:
    getText().contains "boom"

    when:
    error = null // succeed

    then:
    getText() == "ok"

    when:
    error = "kaboom"

    then:
    getText() == "ok" // no need to reload
  }

  def "error in of function is fatal when not implicitly in development"() {
    given:
    implicitlyDevelopment(false)
    server = RatpackServer.of { throw new IllegalStateException("boom") }

    when:
    server.start()

    then:
    thrown IllegalStateException
  }

  def "error in of function is not fatal when implicitly in development"() {
    given:
    server = RatpackServer.of { throw new IllegalStateException("boom") }

    when:
    server.start()

    then:
    noExceptionThrown()
  }

  def "registry build error is fatal when not in development"() {
    given:
    implicitlyDevelopment(false)
    server = RatpackServer.of { it.registry { throw new IllegalStateException("boom") }.handler {} }

    when:
    server.start()

    then:
    thrown IllegalStateException
  }

  def "registry build error is not fatal when in development"() {
    given:
    server = RatpackServer.of {
      it.registry {
        throw new IllegalStateException("boom")
      }.handler {}
    }

    when:
    server.start()

    then:
    noExceptionThrown()
  }

  def "can recover from registry build error when in development"() {
    given:
    implicitlyDevelopment(false)
    def error
    server = RatpackServer.of {
      it.serverConfig(ServerConfig.builder().development(true))
      it.registry {
        if (error) {
          throw new IllegalStateException(error)
        } else {
          Registry.empty()
        }
      }
      it.handler { return { it.render "ok" } as Handler }
    }

    when:
    error = "boom"

    then:
    getText().contains "boom"

    when:
    error = "bang"

    then:
    getText().contains "bang"

    when:
    error = null

    then:
    getText() == "ok"
  }

  def "handler build error is fatal when not in development"() {
    given:
    implicitlyDevelopment(false)
    server = RatpackServer.of {
      it.handler { throw new IllegalStateException("boom") }
    }

    when:
    server.start()

    then:
    thrown IllegalStateException
  }

  def "handler build error is not fatal when in development"() {
    given:
    server = RatpackServer.of {
      it.handler { throw new IllegalStateException("boom") }
    }

    when:
    server.start()

    then:
    noExceptionThrown()
  }

  def "can recover from handler build error when in development"() {
    given:
    implicitlyDevelopment(false)
    def error
    server = RatpackServer.of {
      it.serverConfig(ServerConfig.builder().development(true))
      it.handler {
        if (error) {
          throw new IllegalStateException(error)
        } else {
          return { it.render "ok" } as Handler
        }
      }
    }

    when:
    error = "boom"

    then:
    getText().contains "boom"

    when:
    error = "bang"

    then:
    getText().contains "bang"

    when:
    error = null

    then:
    getText() == "ok"
  }

}
