/*
 * Copyright 2017 the original author or authors.
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

package ratpack.rx2.observable

import io.reactivex.BackpressureStrategy
import io.reactivex.functions.Function
import ratpack.http.client.HttpClient
import ratpack.rx2.RxRatpack
import ratpack.http.client.BaseHttpClientSpec

import static ratpack.rx2.RxRatpack.flow

class RxHttpClientSpec extends BaseHttpClientSpec {

  def setup() {
    RxRatpack.initialize()
  }

  def "can use rx with http client"() {
    given:
    otherApp {
      get("foo") { render "bar" }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        flow(httpClient.get(otherAppUrl("foo")) {}, BackpressureStrategy.BUFFER) map({
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
