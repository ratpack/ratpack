/*
 * Copyright 2016 the original author or authors.
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

package ratpack.http.client

import io.netty.handler.proxy.HttpProxyHandler
import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup

import java.util.concurrent.atomic.AtomicBoolean

import static ratpack.http.Status.*
import static ratpack.http.internal.HttpHeaderConstants.PROXY_AUTHORIZATION

class HttpForwardProxySpec extends BaseHttpClientSpec {

  @AutoCleanup
  EmbeddedApp proxyApp

  EmbeddedApp proxyApp(@DelegatesTo(value = GroovyChain, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    proxyApp = GroovyEmbeddedApp.of {
      registryOf { add ServerErrorHandler, new DefaultDevelopmentErrorHandler() }
      handlers(closure)
    }
  }

  URI proxyAppUrl(String path = "") {
    new URI("$proxyApp.address$path")
  }

  def setup() {
    otherApp {
      get("foo") {
        render "bar"
      }
    }
  }

  def "can make simple proxy request (pooled: #pooled)"() {
    given:
    AtomicBoolean proxied = new AtomicBoolean(false)
    proxyApp {
      all { HttpClient httpClient ->
        proxied.set(true)
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    when:
    bindings {
      bindInstance(HttpClient, HttpClient.of { config -> config
        .poolSize(pooled ? 8 : 0)
        .proxy { p -> p
          .host(proxyApp.address.host)
          .port(proxyApp.address.port)
        }
      })
    }
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "bar"
    proxied.get()

    where:
    pooled << [true, false]
  }

  def "can make simple proxy request with proxy authentication (pooled: #pooled)"() {
    given:
    AtomicBoolean authenticated = new AtomicBoolean(false)
    AtomicBoolean proxied = new AtomicBoolean(false)
    def username = "testUser"
    def password = "testPassword"

    proxyApp {
      all {
        def authHeader = request.headers.get(PROXY_AUTHORIZATION)
        if (authHeader == null) {
          if (authenticated.get()) {
            // The client was already authenticated. In real life, we would have validated the client's session.
            next()
          } else {
            response.status(PROXY_AUTH_REQUIRED).send()
          }
        } else {
          def encodedValue = authHeader.split(" ")[1]
          def decodedValue = new String(encodedValue.decodeBase64())
          def decodedParts = decodedValue.split(":")
          def decodedUsername = decodedParts[0]
          def decodedPassword = decodedParts[1]
          if (decodedUsername != username || decodedPassword != password) {
            context.response.status(PROXY_AUTH_REQUIRED).send()
            return
          }
          authenticated.set(true)
          context.response.status(OK).send()
        }
      }
      all { HttpClient httpClient ->
        proxied.set(true)
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    when:
    bindings {
      bindInstance(HttpClient, HttpClient.of { config -> config
        .poolSize(pooled ? 8 : 0)
        .proxy { p -> p
          .host(proxyApp.address.host)
          .port(proxyApp.address.port)
          .username(username)
          .password(password)
        }
      })
    }
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo")) {
        } onError(HttpProxyHandler.HttpProxyConnectException) {
          context.response.status(INTERNAL_SERVER_ERROR).send()
        } then { ReceivedResponse receivedResponse ->
          response.status(receivedResponse.status)
          response.send(receivedResponse.body.bytes)
        }
      }
    }

    then:
    def response = client.get()
    response.statusCode == 200
    response.body.text == "bar"
    proxied.get()

    where:
    pooled << [true, false]
  }

}
