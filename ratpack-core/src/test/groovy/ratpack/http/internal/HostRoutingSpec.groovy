/*
 * Copyright 2013 the original author or authors.
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

package ratpack.http.internal

import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec

class HostRoutingSpec extends RatpackGroovyDslSpec {

  def "can route by host"() {
    given:
    handlers {
      host("foo.com") {
        all { response.send("Host Handler") }
      }
      all {
        response.send("Default Handler")
      }
    }

    and:
    if (hostValue) {
      requestSpec({ RequestSpec requestSpec ->
        requestSpec.headers.set("Host", hostValue)
      })
    }

    expect:
    def response = getText("abc/def")
    response == expectedResponse

    where:
    hostValue | expectedResponse
    "foo.com" | "Host Handler"
    "car"     | "Default Handler"
    null      | "Default Handler"
  }

}
