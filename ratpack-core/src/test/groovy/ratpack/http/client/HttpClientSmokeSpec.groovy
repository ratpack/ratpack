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

package ratpack.http.client

class HttpClientSmokeSpec extends HttpClientSpec {

  def "can make simple get request"() {
    given:
    otherApp {
      get("foo") { render "bar" }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo")) then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "bar"
  }

  def "can make post request"() {
    given:
    otherApp {
      post("foo") {
        render request.body.text
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.post(otherAppUrl("foo")) { RequestSpec request ->
          request.body.type("text/plain").stream { it << "bar" }
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "bar"
  }

  def "client response buffer is retained for the execution"() {
    given:
    otherApp {
      get {
        render "foo"
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->

        httpClient.get(otherAppUrl()).then {
          def buffer = it.body.buffer
          assert buffer.refCnt() == 2
          blocking { 2 } then {
            assert buffer.refCnt() == 1
            render "bar"
          }

          execution.onComplete {
            assert buffer.refCnt() == 0
          }
        }
      }
    }

    then:
    text == "bar"
  }

}
