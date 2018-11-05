/*
 * Copyright 2018 the original author or authors.
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

import ratpack.error.ServerErrorHandler
import ratpack.handling.Context
import spock.lang.Unroll

import java.time.Duration

@Unroll
class HttpClientIdleTimeoutSpec extends BaseHttpClientSpec {

  def poolingIdleHttpClient = HttpClient.of {
    it.poolSize(1)
    it.idleTimeout(Duration.ofMillis(500))
  }

  def poolingHttpClient = HttpClient.of {
    it.poolSize(1)
  }

  def "test client with idle timeout exceeded"() {
    when:
    otherApp {
      get("1") {
        render "ok"
      }
      get("2") {
        context.execution.sleep(delay).then {
          render "ok"
        }
      }
    }
    bindings {
      bindInstance(ServerErrorHandler, new TestServerErrorHandler())
    }
    handlers {
      get("1") {
        render poolingIdleHttpClient.get("${otherAppUrl()}1".toString().toURI()).map { it.body.text }
      }
      get("2") {
        render poolingIdleHttpClient.get("${otherAppUrl()}2".toString().toURI()).map { it.body.text }
      }
    }
    def r1 = get("1")
    def r2 = get("2")

    then:
    poolingIdleHttpClient.idleTimeout == Duration.ofMillis(500)
    r1.statusCode == 200
    r1.body.text == "ok"
    r2.statusCode == expectedStatus
    r2.body.text.contains(expectedBody)

    where:
    delay                  || expectedStatus || expectedBody
    Duration.ofMillis(0)   || 200            || "ok"
    Duration.ofMillis(750) || 500            || "closed the connection prematurely"
  }

  def "test client without idle timeout"() {
    when:
    otherApp {
      get("1") {
        render "ok"
      }
      get("2") {
        context.execution.sleep(delay).then {
          render "ok"
        }
      }
    }
    bindings {
      bindInstance(ServerErrorHandler, new TestServerErrorHandler())
    }
    handlers {
      get("1") {
        render poolingHttpClient.get("${otherAppUrl()}1".toString().toURI()).map { it.body.text }
      }
      get("2") {
        render poolingHttpClient.get("${otherAppUrl()}2".toString().toURI()).map { it.body.text }
      }
    }
    def r1 = get("1")
    def r2 = get("2")

    then:
    poolingIdleHttpClient.idleTimeout == Duration.ofMillis(500)
    r1.statusCode == 200
    r1.body.text == "ok"
    r2.statusCode == expectedStatus
    r2.body.text.contains(expectedBody)

    where:
    delay                  || expectedStatus || expectedBody
    Duration.ofMillis(0)   || 200            || "ok"
    Duration.ofMillis(750) || 200            || "ok"
  }

  class TestServerErrorHandler implements ServerErrorHandler {

    @Override
    void error(Context context, Throwable throwable) throws Exception {
      context.response.status(500).send(throwable.message)
    }

  }

}
