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

import spock.lang.Timeout

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HttpClientInterceptorSpec extends BaseHttpClientSpec {

  def "can intercept request"() {
    given:

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
      }
    }
    handlers {
      get { HttpClient client ->
        render client.get(otherAppUrl("")).map { it.body.text }
      }
    }

    expect:
    text == 'foo'
  }

  @Timeout(5)
  def "can intercept response"() {
    given:

    def latch = new CountDownLatch(1)

    otherApp {
      get {
        render 'ok'
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
      get { HttpClient client ->
        render client.get(otherAppUrl("")).map { it.body.text }
      }
    }

    when:
    def result = text
    latch.await(5, TimeUnit.SECONDS)

    then:
    result == 'ok'
  }

  @Timeout(5)
  def "can override http client"() {
    given:

    def latch = new CountDownLatch(1)

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
}
