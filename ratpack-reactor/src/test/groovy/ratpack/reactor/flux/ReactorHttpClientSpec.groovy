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

package ratpack.reactor.flux

import ratpack.http.client.BaseHttpClientSpec
import ratpack.http.client.HttpClient
import ratpack.reactor.ReactorRatpack

import java.util.function.Function

import static ratpack.reactor.ReactorRatpack.flux

class ReactorHttpClientSpec extends BaseHttpClientSpec {

  def setup() {
    ReactorRatpack.initialize()
  }

  def "can use rx with http client"() {
    given:
    otherApp {
      get("foo") { render "bar" }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        flux(httpClient.get(otherAppUrl("foo")) {}).map({
          it.body.text.toUpperCase()
        } as Function) subscribe {
          render it
        }
      }
    }

    then:
    text == "BAR"
  }

}
