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

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.util.CharsetUtil
import ratpack.http.internal.HttpHeaderConstants
import ratpack.stream.Streams

import java.time.Duration
import java.util.zip.GZIPInputStream

import static ratpack.http.ResponseChunks.stringChunks
import static ratpack.http.internal.HttpHeaderConstants.CONTENT_ENCODING
import static ratpack.sse.ServerSentEvents.serverSentEvents
import static ratpack.stream.Streams.publish

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
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "bar"
  }

  def "can follow simple redirect get request"() {
    given:
    otherApp {
      get("foo2") {
        redirect(302, otherAppUrl("foo").toString())
      }

      get("foo") {
        render "bar"
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo2")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "bar"
  }

  def "can follow a relative redirect get request"() {
    given:
    otherApp {
      get("foo") {
        response.with {
          status(301)
          headers.set(HttpHeaderConstants.LOCATION, "/tar")
          send()
        }
      }
      get("tar") { render "tar" }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "tar"
  }


  def "Do not follow simple redirect if redirects set to 0"() {
    given:
    otherApp {
      get("foo2") {
        redirect(302, otherAppUrl("foo").toString())
      }

      get("foo") {
        render "bar"
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo2")) {
          it.redirects(0)
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == ""
  }

  def "Stop redirects in loop"() {
    given:
    otherApp {
      get("foo2") {
        redirect(302, otherAppUrl("foo").toString())
      }

      get("foo") {
        redirect(302, otherAppUrl("foo2").toString())
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo2")) {
        } then { ReceivedResponse response ->
          render "Status: " + response.statusCode
        }
      }
    }

    then:
    text == "Status: 302"
  }


  def "can make post request"() {
    given:
    otherApp {
      post("foo") {
        request.body.then { body ->
          render body.text
        }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def respProm = httpClient.post(otherAppUrl("foo")) {
          it.body.type("text/plain").text("bar")
        }
        respProm.onError { t ->
          t.printStackTrace()
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
        httpClient.get(otherAppUrl()) {
        } then {
          def buffer = it.body.buffer
          assert buffer.refCnt() == 1
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
        request.body.then { body ->
          render body.text
        }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.post(otherAppUrl()) {
          it.body {
            it.buffer(Unpooled.copiedBuffer("foo", CharsetUtil.UTF_8))
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
        request.body.then { body ->
          render body.text
        }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.post(otherAppUrl()) {
          it.body {
            it.bytes("foo".getBytes(CharsetUtil.UTF_8))
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
        httpClient.get(otherAppUrl()) {
          it.headers {
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
        httpClient.get(otherAppUrl()) {
        } then {
          it.send(response)
        }
      }
    }

    then:
    text == "abc123"
    response.headers.get(HttpHeaders.Names.CONTENT_TYPE) == "text/plain;charset=UTF-8"
  }

  def "can send request body as text"() {
    given:
    otherApp {
      post {
        request.body.then { body ->
          assert body.contentType.toString() == "text/plain;charset=UTF-8"
          render body.text
        }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.post(otherAppUrl()) {
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
        request.body.then { body ->
          render body.contentType.toString()
        }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.post(otherAppUrl()) {
          it.body.type("application/json").text("{'foo': 'bar'}")
        } then {
          render it.body.text
        }
      }
    }

    then:
    getText() == "application/json"
  }

  def "500 Error when RequestSpec throws an exception"() {
    given:
    otherApp {}

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl()) {
          throw new Exception("Some failure in the RequestSpec")
        } then {
          render it.body.text
        }
      }
    }

    when:
    get()

    then:
    response.statusCode == 500
  }

  def "can set read timeout"() {
    when:
    otherApp {
      get { ctx ->
        def stream = Streams.periodically(ctx, Duration.ofSeconds(5)) {
          it < 5 ? "a" : null
        }

        render serverSentEvents(stream) {
          it.id("a")
        }
      }
    }

    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl()) {
          it.readTimeoutSeconds(1)
        } onError {
          render it.class.name
        } then {
          render "success"
        }
      }
    }

    then:
    text == ReadTimeoutException.name
  }

  def "can directly stream a client chunked response"() {
    given:
    otherApp {
      get("foo") {
        render stringChunks(
          publish(["bar"] * 3)
        )
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl("foo")) {
        } then { StreamedResponse responseStream ->
          responseStream.send(response)
        }
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
transfer-encoding: chunked

3
bar
3
bar
3
bar
0

"""
  }

  def "can modify the stream of a client chunked response"() {
    given:
    otherApp {
      get("foo") {
        render stringChunks(
          publish(["bar"] * 3)
        )
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl("foo")) {
        } then { StreamedResponse stream ->
          render stringChunks(
            stream.body.map {
              it.toString(CharsetUtil.UTF_8).toUpperCase()
            }
          )
        }
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 200 OK
transfer-encoding: chunked
content-type: text/plain;charset=UTF-8

3
BAR
3
BAR
3
BAR
0

"""
  }

  def "can follow a redirect when streaming a client response"() {
    given:
    otherApp {
      get("foo2") {
        redirect(302, otherAppUrl("foo").toString())
      }

      get("foo") {
        render "bar"
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl("foo2")) {
        } then { StreamedResponse responseStream ->
          responseStream.send(response)
        }
      }
    }

    then:
    text == "bar"
  }

  def "can decompress a compressed response"() {
    given:
    requestSpec {
      it.decompressResponse(false) // tell test http client to not decompress the response
      it.headers {
        it.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP)
      }
    }

    and:
    otherApp {
      get("foo") {
        response.send("bar")
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.request(otherAppUrl("foo")) { RequestSpec rs ->
          rs.headers.set("accept-encoding", "compress, gzip")
        } then { ReceivedResponse receivedResponse ->
          receivedResponse.send(response)
        }
      }
    }

    when:
    def response = get()

    then:
    response.headers.get(CONTENT_ENCODING) == null
    response.body.text == "bar"
  }

  def "can not decompress a compressed response"() {
    given:
    requestSpec {
      it.decompressResponse(false) // tell test http client to not decompress the response
      it.headers {
        it.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP)
      }
    }

    and:
    otherApp {
      get("foo") {
        response.send("bar")
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.request(otherAppUrl("foo")) { RequestSpec rs ->
          rs.headers.set("accept-encoding", "compress, gzip")
          rs.decompressResponse(false)
        } then { ReceivedResponse receivedResponse ->
          receivedResponse.send(response)
        }
      }
    }

    when:
    def response = get()

    then:
    response.headers.get(CONTENT_ENCODING) == "gzip"
    new GZIPInputStream(response.body.inputStream).bytes == "bar".bytes
  }

  def "can decompress a streamed compressed response"() {
    given:
    requestSpec {
      it.decompressResponse(false) // tell test http client to not decompress the response
      it.headers {
        it.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP)
      }
    }

    and:
    otherApp {
      get("foo") {
        response.send("bar")
      }
    }

    and:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl("foo")) { rs ->
          rs.headers.set("accept-Encoding", "compress, gzip")
        } then { StreamedResponse streamedResponse ->
          response.getHeaders().copy(streamedResponse.headers)
          response.sendStream(streamedResponse.body)
        }
      }
    }

    when:
    def response = get()

    then:
    response.headers.get(CONTENT_ENCODING) == null
    response.body.text == "bar"
  }
}
