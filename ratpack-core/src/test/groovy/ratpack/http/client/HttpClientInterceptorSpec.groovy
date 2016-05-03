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
import spock.lang.Unroll

/**
 * Test suite for request/response interceptors for {@link HttpClient}
 */
class HttpClientInterceptorSpec extends HttpClientSpec {

  @Unroll
  def "can intercept #method request and response"() {
    given:
    otherApp {
      get("foo") { ctx ->
        render "bar"
      }
    }
    def requestInterceptor = Mock(HttpClientRequestInterceptor)
    def responseInterceptor = Mock(HttpClientResponseInterceptor)
    when:

    handlers {
      get { ctx ->
        ctx.execution.add(HttpClientRequestInterceptor, requestInterceptor)
        ctx.execution.add(HttpClientResponseInterceptor, responseInterceptor)
        def httpClient = ctx.get(HttpClient)
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }
    text
    then:
    1 * requestInterceptor.intercept(_ as SentRequest)
    1 * responseInterceptor.intercept(_ as ReceivedResponse)
    where:
    method << ["GET", "PUT", "POST", "DELETE", "PATCH"]
  }

  @Unroll
  def "can intercept #method request and response with client factory"() {
    given:
    otherApp {
      get("foo") { ctx ->
        render "bar"
      }
    }
    def requestInterceptor = Mock(HttpClientRequestInterceptor)
    def responseInterceptor = Mock(HttpClientResponseInterceptor)
    when:

    handlers {
      get { ctx ->
        def factory = ctx.get(HttpClientFactory)
        def httpClient = factory.create(requestInterceptor, responseInterceptor)
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }
    text
    then:
    1 * requestInterceptor.intercept(_ as SentRequest)
    1 * responseInterceptor.intercept(_ as ReceivedResponse)
    where:
    method << ["GET", "PUT", "POST", "DELETE", "PATCH"]
  }
}
