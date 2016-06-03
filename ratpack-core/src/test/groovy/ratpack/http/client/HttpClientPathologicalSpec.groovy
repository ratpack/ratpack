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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.Fault
import ratpack.http.client.internal.PooledHttpClientFactory
import ratpack.http.client.internal.PooledHttpConfig
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Timeout
import spock.lang.Unroll

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

@Unroll
class HttpClientPathologicalSpec extends RatpackGroovyDslSpec implements PooledHttpClientFactory {

  WireMockServer wm

  def setup() {
    wm = new WireMockServer(wireMockConfig().dynamicPort())
    wm.start()
  }

  @Timeout(5)
  void "handles empty response"() {
    given:
    wm.stubFor(WireMock.get(WireMock.urlEqualTo('/test')).willReturn(WireMock.aResponse().withFault(Fault.EMPTY_RESPONSE)))
    def mockServiceUrl = "http://localhost:${wm.port()}/test"

    when:
    handlers {
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
        render httpClient.get(URI.create(mockServiceUrl))
          .map { "You'll never see this" }
          .mapError { it.message }
      }
    }

    then:
    text == "Server $mockServiceUrl closed the connection prematurely"

    where:
    pooled << [true, false]
  }
}
