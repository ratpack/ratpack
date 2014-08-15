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

import org.apache.http.HttpHeaders
import ratpack.http.HttpUrlSpec
import ratpack.util.internal.IoUtils

class HttpClientSmokeSpec extends HttpClientSpec {

  def "can make simple get request"() {
    given:
    otherApp {
      get("foo") {
        render "bar"
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get({ RequestSpec request ->
          request.url { HttpUrlSpec httpUrlSpec ->
            httpUrlSpec.set(otherAppUrl("foo"))
          }
        }) then { ReceivedResponse response ->
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
        def respProm = httpClient.post { RequestSpec request ->
          request.url { HttpUrlSpec httpUrlSpec ->
            httpUrlSpec.set(otherAppUrl("foo"))
          }
          request.body.type("text/plain").stream { it << "bar" }
        }

        respProm.onError({ Throwable t ->
          t.printStackTrace()
        })

        respProm.then { ReceivedResponse response ->
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
        httpClient.get { RequestSpec request ->
          request.url { HttpUrlSpec httpUrlSpec ->
            httpUrlSpec.set(otherAppUrl())
          }
        } then {
          def buffer = it.body.buffer
          assert buffer.refCnt() == 2
          blocking { 2 } then {
            assert buffer.refCnt() == 1
            render "bar"
          }

          execution.onCleanup {
            assert buffer.refCnt() == 0
          }
        }
      }
    }

    then:
    text == "bar"
  }

  def "can write body using buffer"() {
    given:
    otherApp {
      post {
        render request.body.text
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.post {
          it.
            url {
              it.set(otherAppUrl())
            }.
            body {
              it.buffer(IoUtils.utf8Buffer("foo"))
            }
        } then {
          render it.body.text
        }
      }
    }

    then:
    text == "foo"
  }

  def "can write body using bytes"() {
    given:
    otherApp {
      post {
        render request.body.text
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.post {
          it.
            url {
              it.set(otherAppUrl())
            }.
            body {
              it.bytes(IoUtils.utf8Bytes("foo"))
            }
        } then {
          render it.body.text
        }
      }
    }

    then:
    text == "foo"
  }

  def "can set headers"() {
    given:
    otherApp {
      get {
        render request.headers.foo
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get {
          it.
            url {
              it.set(otherAppUrl())
            }.
            headers {
              it.add("foo", "bar")
            }
        } then {
          render it.body.text
        }
      }
    }

    then:
    text == "bar"
  }

  def "can serve response body buffer"() {
    given:
    otherApp {
      get {
        render "abc123"
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get {
          it.url.set(otherAppUrl())
        } then {
          it.send(response)
        }
      }
    }

    then:
    text == "abc123"
    response.headers.get(HttpHeaders.CONTENT_TYPE)  == "text/plain;charset=UTF-8"
  }

  def "can send request body as text"() {
    given:
    otherApp {
      post {
        assert request.body.contentType.toString() == "text/plain;charset=UTF-8"
        render request.body.text
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.post {
          it.url.set(otherAppUrl())
          it.body.text("føø")
        } then {
          render it.body.text
        }
      }
    }

    then:
    getText() == "føø"
  }

  def "can send request body as text of content type"() {
    given:
    otherApp {
      post {
        render request.body.contentType.toString()
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.post {
          it.url.set(otherAppUrl())
          it.body.type("application/json").text("{'foo': 'bar'}")
        } then {
          render it.body.text
        }
      }
    }

    then:
    getText() == "application/json"
  }

}
