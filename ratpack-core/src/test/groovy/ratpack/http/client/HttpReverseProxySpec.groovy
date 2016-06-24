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

import ratpack.http.client.internal.PooledHttpClientFactory
import ratpack.http.client.internal.PooledHttpConfig
import spock.lang.Unroll

@Unroll
class HttpReverseProxySpec extends HttpClientSpec implements PooledHttpClientFactory {

  def "can forward request body"() {
    when:
    otherApp {
      post {
        render request.body.map { "received: " + it.text }
      }
    }
    handlers {
      post {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
        request.body.then { body ->
          httpClient.requestStream(otherAppUrl()) {
            it.method "POST"
            it.body.buffer body.buffer
          } then {
            it.forwardTo(response)
          }
        }
      }
    }

    then:
    def r = request {
      it.method "POST"
      it.body.text "foo"
    }

    r.body.text == "received: foo"

    where:
    pooled << [true, false]
  }

}
