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

import io.netty.channel.Channel
import ratpack.error.ServerErrorHandler
import ratpack.handling.Context
import spock.lang.Unroll

import java.time.Duration

@Unroll
class HttpClientPoolingReadTimeoutSpec extends BaseHttpClientSpec {

  def nonPoolingReadTimeoutHttpClient = HttpClient.of {
    it.poolSize(0)
    it.readTimeout(Duration.ofMillis(500))
  }

  def poolingReadTimeoutHttpClient = HttpClient.of {
    it.poolSize(1)
    // Somehow a read timeout is forcing a close request to the server
    it.readTimeout(Duration.ofMillis(500))
  }

  def "test non pooling client with read timeout exceeded"() {
    given:
    Channel channel1 = null
    Channel channel2 = null
    def delay = Duration.ofMillis(1000)
    def expectedStatus = 500
    def expectedBody = "Read timeout (PT0.5S) waiting on HTTP server"

    when:
    otherApp {
      get("1") {
        channel1 = directChannelAccess.channel
        context.execution.sleep(delay).then {
          render "ok"
        }
      }
      get("2") {
        channel2 = directChannelAccess.channel
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
        nonPoolingReadTimeoutHttpClient.get("${otherAppUrl()}1".toString().toURI()).then { render it.body.text }
      }
      get("2") {
        nonPoolingReadTimeoutHttpClient.get("${otherAppUrl()}2".toString().toURI()).then { render it.body.text }
      }
    }
    def r1 = get("1")
    def r2 = get("2")

    then:
    r1.statusCode == expectedStatus
    r1.body.text.startsWith(expectedBody)
    r2.statusCode == expectedStatus
    r2.body.text.startsWith(expectedBody)
    !channel1.open
    !channel2.open
  }

  def "test pooling client with read timeout exceeded"() {
    given:
    Channel channel1 = null
    Channel channel2 = null
    def delay = Duration.ofMillis(1000)
    def expectedStatus = 500
    def expectedBody = "Read timeout (PT0.5S) waiting on HTTP server"

    when:
    otherApp {
      get("1") {
        channel1 = directChannelAccess.channel
        context.execution.sleep(delay).then {
          render "ok"
        }
      }
      get("2") {
        channel2 = directChannelAccess.channel
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
        poolingReadTimeoutHttpClient.get("${otherAppUrl()}1".toString().toURI()).then { render it.body.text }
      }
      get("2") {
        poolingReadTimeoutHttpClient.get("${otherAppUrl()}2".toString().toURI()).then { render it.body.text }
      }
    }
    def r1 = get("1")
    Thread.sleep(delay.toMillis())
    def r2 = get("2")

    then:
    this.server.running
    r1.statusCode == expectedStatus
    r1.body.text.startsWith(expectedBody)
    r2.statusCode == expectedStatus
    r2.body.text.startsWith(expectedBody)
    channel1.open
    channel2.open
  }

  class TestServerErrorHandler implements ServerErrorHandler {

    @Override
    void error(Context context, Throwable throwable) throws Exception {
      context.response.status(500).send(throwable.message)
    }

  }

}
