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

package ratpack.core.http.client

import ratpack.core.error.ServerErrorHandler
import ratpack.core.error.internal.DefaultDevelopmentErrorHandler
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.client.BaseHttpClientSpec
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup

import java.util.concurrent.atomic.AtomicBoolean

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

  def "can make simple proxy request"() {
    given:
    AtomicBoolean proxied = new AtomicBoolean(false)
    otherApp {
      get("foo") {
        render "bar"
      }
    }
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

}
