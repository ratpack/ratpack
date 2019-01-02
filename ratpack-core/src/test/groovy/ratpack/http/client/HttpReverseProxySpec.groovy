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

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.stream.Streams

import java.util.zip.GZIPInputStream

import static ratpack.http.ResponseChunks.stringChunks
import static ratpack.http.internal.HttpHeaderConstants.CONTENT_ENCODING
import static ratpack.stream.Streams.publish

class HttpReverseProxySpec extends BaseHttpClientSpec {

  def "can forward non streamed response as a stream"() {
    when:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      post {
        render request.body.map { "received: " + it.text }
      }
    }
    handlers {
      post { HttpClient httpClient ->
        request.body.then { body ->
          httpClient.requestStream(otherAppUrl()) {
            it.post().body.bytes body.bytes
          } then {
            it.forwardTo(response)
          }
        }
      }
    }

    then:
    def r = request {
      it.post().body.text "foo"
    }

    r.body.text == "received: foo"

    where:
    pooled << [true, false]
  }

  def "can forward chunked response as a stream"() {
    when:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      post {
        request.body.then { t ->
          render stringChunks("text/plain", Streams.yield { it.requestNum < 1000 ? t.text : null })
        }
      }
    }
    handlers {
      post { HttpClient httpClient ->
        request.body.then { body ->
          httpClient.requestStream(otherAppUrl()) {
            it.post().body.buffer body.buffer
          } then {
            it.forwardTo(response)
          }
        }
      }
    }

    then:
    def r = request {
      it.post().body.text "foo"
    }

    r.body.text == "foo" * 1000

    where:
    pooled << [true, false]
  }

  def "can forward an EOF framed response"() {
    when:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      post {
        request.body.then {
          response.forceCloseConnection()
          response.sendStream(publish([it.text] * 1000).map { Unpooled.wrappedBuffer(it.bytes) })
        }
      }
    }
    handlers {
      post { HttpClient httpClient ->
        request.body.then { body ->
          httpClient.requestStream(otherAppUrl()) {
            it.post().body.buffer body.buffer
          } then {
            it.forwardTo(response)
          }
        }
      }
    }

    then:
    def r = request {
      it.post().body.text "foo"
    }

    r.body.text == "foo" * 1000

    where:
    pooled << [false]
  }

  def "can proxy a client response"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo") {
        response.headers.add("x-foo-header", "foo")
        render "bar"
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl("foo")) {
        } then {
          it.forwardTo(response)
        }
      }
    }

    expect:
    def response = get()
    response.headers.asMultiValueMap() == ["x-foo-header": "foo", "content-type": "text/plain;charset=UTF-8", "content-length": "3"]
    response.body.text == "bar"

    where:
    pooled << [true, false]
  }

  def "can proxy a client chunked response"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo") {
        response.headers.add("x-foo-header", "foo")
        render stringChunks(
          publish(["bar"] * 3)
        )
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl("foo")) {
        } then {
          it.forwardTo(response)
        }
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 200 OK
x-foo-header: foo
transfer-encoding: chunked
content-type: text/plain;charset=UTF-8
connection: close

3
bar
3
bar
3
bar
0

"""

    where:
    pooled << [true, false]
  }

  def "can mutate response headers while proxying"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo") {
        response.headers.add("x-foo-header", "foo")
        render stringChunks(
          publish(["bar"] * 3)
        )
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl("foo")) {
        } then {
          it.forwardTo(response) { it.remove("x-foo-header").add("x-bar-header", "bar") }
        }
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 200 OK
transfer-encoding: chunked
content-type: text/plain;charset=UTF-8
x-bar-header: bar
connection: close

3
bar
3
bar
3
bar
0

"""

    where:
    pooled << [true, false]
  }

  def "can proxy a client error"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo") {
        response.headers.add("x-foo-header", "foo")
        clientError(404)
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl("foo")) {
        } then {
          it.forwardTo(response)
        }
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 404 Not Found
x-foo-header: foo
content-type: text/plain;charset=UTF-8
content-length: 16
connection: close

Client error 404"""

    where:
    pooled << [true, false]
  }

  def "can proxy a server error"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo") {
        response.headers.add("x-foo-header", "foo")
        error(new Throwable("A server error occurred"))
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl("foo")) {
        } then {
          it.forwardTo(response)
        }
      }
    }

    expect:
    rawResponse().with {
      startsWith("""HTTP/1.1 500 Internal Server Error
x-foo-header: foo
content-type: text/plain
transfer-encoding: chunked
connection: close
""")
      contains("A server error occurred")
    }

    where:
    pooled << [true, false]
  }

  def "can proxy compressed responses"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo") {
        render "bar"
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.request(otherAppUrl("foo")) {
          it.decompressResponse(false)
            .headers.copy(request.headers)
        } then {
          it.forwardTo(response)
        }
      }
    }

    when:
    def response = requestSpec {
      it.decompressResponse(false)
        .headers.add("Accept-Encoding", "compress, gzip")
    }.get()

    then:
    response.headers.get(CONTENT_ENCODING) == "gzip"
    new GZIPInputStream(response.body.inputStream).bytes == "bar".bytes

    where:
    pooled << [true, false]
  }

  def "can proxy a post request"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      post("foo") {
        render request.body.map { it.text }
      }
    }

    and:
    handlers {
      post { HttpClient httpClient ->
        request.body.flatMap { incoming ->
          httpClient.request(otherAppUrl("foo")) { it.post().body.buffer(incoming.buffer) }
        } then {
          it.forwardTo(response)
        }
      }
    }

    when:
    def response = requestSpec { it.body.text("bar") }.post()

    then:
    response.body.text == "bar"

    where:
    pooled << [true, false]
  }

  def "does not add content length for empty upstream #status"() {
    when:
    otherApp {
      get {
        response.status(status.code()).send()
      }
    }

    handlers {
      get {
        get(HttpClient).requestStream(otherAppUrl(), {}).then {
          it.forwardTo(response)
        }
      }
    }

    then:
    rawResponse() == """HTTP/1.1 ${status}
connection: close

"""

    where:
    status << noBodyResponseStatuses()
  }

  static List<HttpResponseStatus> noBodyResponseStatuses() {
    [HttpResponseStatus.valueOf(100), HttpResponseStatus.valueOf(150), HttpResponseStatus.valueOf(199), HttpResponseStatus.valueOf(204), HttpResponseStatus.valueOf(304)]
  }

}
