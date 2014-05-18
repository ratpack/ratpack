/*
 * Copyright 2014 the original author or authors.
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

package ratpack.hystrix

import ratpack.http.client.HttpClientSpec
import ratpack.http.client.ReceivedResponse
import ratpack.rx.RxRatpack
import spock.lang.Unroll

import static ratpack.hystrix.CommandFactory.*

class HystrixRequestCachingSpec extends HttpClientSpec {

  def setup() {
    RxRatpack.initialize()
    HystrixRatpack.initialize()
  }

  @Unroll
  def "can cache #executionType command execution"() {
    given:
    otherApp {
      get("foo/:id") { render pathTokens.get("id") }
    }

    and:
    handlers {
      handler("blocking") {
        def firstCall = hystrixCommand("1").queue()
        blocking {
          firstCall.get()
        } then {
          def secondCall = hystrixCommand("2").queue()
          blocking {
            secondCall.get()
          } then {
            render it
          }
        }
      }

      handler("observable") {
        hystrixObservableCommand("1").flatMap {
          hystrixObservableCommand("2")
        } subscribe {
          render it
        }
      }

      handler("blocking-observable") {
        hystrixBlockingObservableCommand("1").flatMap {
          hystrixBlockingObservableCommand("2")
        } subscribe {
          render it
        }
      }

      handler("http-observable") {
        hystrixObservableHttpCommand(otherAppUrl("foo/1")).flatMap {
          hystrixObservableHttpCommand(otherAppUrl("foo/2")).map { ReceivedResponse resp ->
            return resp.body.text
          }
        } subscribe {
          render it
        }
      }
    }

    expect:
    getText(executionType) == "1"

    where:
    executionType << ["blocking", "observable", "blocking-observable", "http-observable"]
  }

}
