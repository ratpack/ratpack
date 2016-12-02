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

import ratpack.exec.Operation
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HttpClientInterceptorSpec extends BaseHttpClientSpec {

  @Timeout(5)
  def "can intercept requests and responses"() {
    given:

    def latch = new CountDownLatch(1)

    otherApp {
      get {
        render request.headers.get('X-GLOBAL')
      }
    }

    bindings {
      bindInstance HttpClient, HttpClient.of { spec ->
        spec.requestIntercept { request ->
          request.headers { headers ->
            headers.add 'X-GLOBAL', 'foo'
          }
        }
        spec.responseIntercept { response ->
          latch.countDown()
        }
      }
    }
    handlers {
      get { HttpClient client ->
        render client.get(otherAppUrl("")).map { it.body.text }
      }
    }

    when:
    def result = text
    latch.await(5, TimeUnit.SECONDS)

    then:
    result == 'foo'
  }

  @Timeout(5)
  def "can override http client"() {
    given:

    def latch = new CountDownLatch(2)

    otherApp {
      get {
        render request.headers.get('X-GLOBAL')
      }
    }

    bindings {
      bindInstance HttpClient, HttpClient.of { spec ->
        spec.responseIntercept { response ->
          latch.countDown()
        }
      }
    }
    handlers {
      all { HttpClient client ->
        next single(client.copyWith { spec ->
          spec.requestIntercept { request ->
            request.headers { headers ->
              headers.add 'X-GLOBAL', 'foo'
            }
          }
          spec.responseIntercept { response ->
            latch.countDown()
          }
        })
      }
      get { HttpClient client ->
        render client.get(otherAppUrl("")).map { it.body.text }
      }
    }

    when:
    def result = text
    latch.await(5, TimeUnit.SECONDS)

    then:
    result == 'foo'
  }

  @Timeout(5)
  def "can intercept streamed requests and responses"() {
    given:
    def payload = new byte[15 * 8012]
    new Random().nextBytes(payload)
    def latch = new CountDownLatch(1)

    bindings {
      bindInstance HttpClient, HttpClient.of { spec ->
        spec.responseIntercept { response ->
          latch.countDown()
        }
      }
    }

    when:
    otherApp {
      get {
        response.send(payload)
      }
    }
    handlers {
      get { HttpClient http ->
        http.requestStream(otherAppUrl(), {}).then { StreamedResponse streamedResponse ->
          streamedResponse.forwardTo(response)
        }
      }
    }

    then:
    (1..10).collect { get().body.bytes == payload }.every()
    latch.await(5, TimeUnit.SECONDS)
  }

  @Timeout(5)
  def "can execute promises in response interceptor"() {
    given:
    def latch = new CountDownLatch(1)

    otherApp {
      get {
        render 'foo'
      }
    }

    bindings {
      bindInstance HttpClient, HttpClient.of { spec ->
        spec.responseIntercept { response ->
          Operation.of {
            latch.countDown()
          }.then()
        }
      }
    }

    handlers {
      get { HttpClient client ->
        render client.get(otherAppUrl("")).map { it.body.text }
      }
    }

    when:
    def result = text
    latch.await(5, TimeUnit.SECONDS)

    then:
    result == 'foo'
  }

  @Timeout(5)
  def "can execute promises in streaming response interceptor"() {
    given:
    def payload = new byte[15 * 8012]
    new Random().nextBytes(payload)
    def latch = new CountDownLatch(1)

    bindings {
      bindInstance HttpClient, HttpClient.of { spec ->
        spec.responseIntercept { response ->
          Operation.of {
            latch.countDown()
          }.then()
        }
      }
    }

    when:
    otherApp {
      get {
        response.send(payload)
      }
    }
    handlers {
      get { HttpClient http ->
        http.requestStream(otherAppUrl(), {}).then { StreamedResponse streamedResponse ->
          streamedResponse.forwardTo(response)
        }
      }
    }

    then:
    (1..10).collect { get().body.bytes == payload }.every()
    latch.await(5, TimeUnit.SECONDS)
  }
}
