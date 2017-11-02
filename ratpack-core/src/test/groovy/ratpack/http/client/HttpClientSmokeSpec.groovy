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
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaders
import io.netty.util.CharsetUtil
import ratpack.exec.Blocking
import ratpack.exec.ExecController
import ratpack.stream.Streams
import spock.lang.IgnoreIf
import spock.util.concurrent.BlockingVariable
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.zip.GZIPInputStream

import static ratpack.http.ResponseChunks.stringChunks
import static ratpack.http.internal.HttpHeaderConstants.CONTENT_ENCODING
import static ratpack.sse.ServerSentEvents.serverSentEvents
import static ratpack.stream.Streams.publish

class HttpClientSmokeSpec extends BaseHttpClientSpec {

  PollingConditions polling = new PollingConditions()

  def "can make simple get request"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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

    where:
    pooled << [true, false]
  }

  def "can make post request"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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

    where:
    pooled << [true, false]
  }

  def "client response buffer is retained for the execution"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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
          Blocking.get { 2 } then {
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

    where:
    pooled << [true, false]
  }

  def "can write body using buffer"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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

    where:
    pooled << [true, false]
  }

  def "can write body using bytes"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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

    where:
    pooled << [true, false]
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
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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
          it.forwardTo(response)
        }
      }
    }

    then:
    text == "abc123"
    response.headers.get(HttpHeaders.Names.CONTENT_TYPE) == "text/plain;charset=UTF-8"

    where:
    pooled << [true, false]
  }

  def "can send request body as text"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      post {
        request.body.then { body ->
          assert body.contentType.toString() == "text/plain; charset=utf-8"
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

    where:
    pooled << [true, false]
  }

  def "can send request body as text of content type"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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

    where:
    pooled << [true, false]
  }

  def "500 Error when RequestSpec throws an exception"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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

    where:
    pooled << [true, false]
  }

  @IgnoreIf({ InetAddress.localHost.isLoopbackAddress() })
  def "can set default connect timeout"() {
    setup:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) ; it.connectTimeout(Duration.ofMillis(20)) })
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get("http://netty.io:65535".toURI()) onError {
          render it.toString()
        } then {
          render "success!"
        }
      }
    }

    then:
    text == "io.netty.channel.ConnectTimeoutException: Connect timeout (PT0.02S) connecting to http://netty.io:65535"

    where:
    pooled << [true, false]
  }

  @IgnoreIf({ InetAddress.localHost.isLoopbackAddress() })
  def "can override connect timeout on request"() {
    setup:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get("http://netty.io:65535".toURI()) {
          it.connectTimeout(Duration.ofMillis(20))
        } onError {
          render it.toString()
        } then {
          render "success"
        }
      }
    }

    then:
    text == "io.netty.channel.ConnectTimeoutException: Connect timeout (PT0.02S) connecting to http://netty.io:65535"

    where:
    pooled << [true, false]
  }

  def "can set read timeout from pooling config"() {
    setup:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }

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
          it.readTimeout(Duration.ofSeconds(1))
        } onError {
          render it.toString()
        } then {
          render "success"
        }
      }
    }

    then:
    text == "ratpack.http.client.HttpClientReadTimeoutException: Read timeout (PT1S) waiting on HTTP server at $otherApp.address".toString()

    where:
    pooled << [true, false]
  }

  def "can set read timeout on request"() {
    setup:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }

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
        httpClient.get(otherAppUrl(), { it.readTimeout(Duration.ofSeconds(1)) }).onError {
          render it.toString()
        } then {
          render "success"
        }
      }
    }

    then:
    text == "ratpack.http.client.HttpClientReadTimeoutException: Read timeout (PT1S) waiting on HTTP server at $otherApp.address".toString()

    where:
    pooled << [true, false]
  }

  def "can directly stream a client chunked response"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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
          responseStream.forwardTo(response)
        }
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 200 OK
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

  def "can modify the stream of a client chunked response"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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
              def string = it.toString(CharsetUtil.UTF_8)
              it.release()
              string.toUpperCase()
            }
          )
        }
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 200 OK
transfer-encoding: chunked
content-type: text/plain;charset=UTF-8
connection: close

3
BAR
3
BAR
3
BAR
0

"""

    where:
    pooled << [true, false]
  }

  def "can follow a redirect when streaming a client response"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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
          responseStream.forwardTo(response)
        }
      }
    }

    then:
    text == "bar"

    where:
    pooled << [true, false]
  }

  def "can decompress a compressed response"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    requestSpec {
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
          receivedResponse.forwardTo(response)
        }
      }
    }

    when:
    def response = get()

    then:
    response.headers.get(CONTENT_ENCODING) == null
    response.body.text == "bar"

    where:
    pooled << [true, false]
  }

  def "can not decompress a compressed response"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
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
          receivedResponse.forwardTo(response)
        }
      }
    }

    when:
    def response = get()

    then:
    response.headers.get(CONTENT_ENCODING) == "gzip"
    new GZIPInputStream(response.body.inputStream).bytes == "bar".bytes

    where:
    pooled << [true, false]
  }

  def "can decompress a streamed compressed response"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    requestSpec {
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

    where:
    pooled << [true, false]
  }

  def "can configure request method #method via request spec"() {
    given:
    handlers {
      path("foo") {
        byMethod {
          get {
            response.send "GET-foo"
          }
          put {
            response.send "PUT-foo"
          }
          post {
            response.send "POST-foo"
          }
          delete {
            response.send "DELETE-foo"
          }
          patch {
            response.send "PATCH-foo"
          }
        }
      }
      all {
        byMethod {
          get {
            response.send "GET"
          }
          put {
            response.send "PUT"
          }
          post {
            response.send "POST"
          }
          delete {
            response.send "DELETE"
          }
          patch {
            response.send "PATCH"
          }
        }
      }

    }

    when:
    def response = request { spec ->
      spec.method(method)
    }
    def pathResponse = request("foo") { spec ->
      spec.method(method)
    }

    then:
    response.status.code == 200
    response.body.text == method

    and:
    pathResponse.status.code == 200
    pathResponse.body.text == "${method}-foo"

    where:
    method << ["GET", "PUT", "POST", "DELETE", "PATCH"]
  }

  def "can configure request method OPTIONS via request spec"() {
    given:
    handlers {
      path("foo") {
        byMethod {
          get {
            response.send "GET-foo"
          }
        }
      }
      all {
        byMethod {
          get {
            response.send "GET"
          }
          put {
            response.send "PUT"
          }
          post {
            response.send "POST"
          }
          delete {
            response.send "DELETE"
          }
          patch {
            response.send "PATCH"
          }
        }
      }

    }

    when:
    def response = request { spec ->
      spec.options()
    }
    def pathResponse = request("foo") { spec ->
      spec.options()
    }

    then:
    response.status.code == 200
    response.headers.get("ALLOW") == "DELETE,GET,PATCH,POST,PUT"

    and:
    pathResponse.status.code == 200
    pathResponse.headers.get("ALLOW") == "GET"
  }

  def "can configure request method HEAD via request spec"() {
    given:
    handlers {
      get {
        response.send "GET"
      }
    }

    when:
    def response = request { spec ->
      spec.head()
    }
    def pathResponse = request("foo") { spec ->
      spec.head()
    }

    then:
    response.status.code == 200

    and:
    pathResponse.status.code == 404
  }

  def "can track http client metrics when pooling is disabled"() {
    given:
    String ok = 'ok'
    def result = new BlockingVariable<String>()
    def httpClient = HttpClient.of {
      it.poolSize(0)
      it.enableMetricsCollection(true)
    }

    bindings {
      bindInstance(HttpClient, httpClient)
    }

    when:
    otherApp {
      get {
        Blocking.get({
          return result.get()
        })
          .onError(it.&error)
          .then(it.&render)
      }
    }

    then:
    assert httpClient.getHttpClientStats().totalActiveConnectionCount == 0
    assert httpClient.getHttpClientStats().totalIdleConnectionCount == 0
    assert httpClient.getHttpClientStats().totalConnectionCount == 0

    when:
    handlers {
      get {
        ExecController execController = it.get(ExecController)
        execController.fork().start({
          httpClient.get(otherAppUrl())
            .then({ val ->
            assert val.body.text == ok
          })
        })
        render ok
      }
    }

    then:
    text == "ok"

    polling.within(2) {
      assert httpClient.getHttpClientStats().totalActiveConnectionCount == 1
      assert httpClient.getHttpClientStats().totalIdleConnectionCount == 0
      assert httpClient.getHttpClientStats().totalConnectionCount == 1
    }

    when:
    result.set(ok)

    then:
    polling.within(2) {
      assert httpClient.getHttpClientStats().totalActiveConnectionCount == 0
      assert httpClient.getHttpClientStats().totalIdleConnectionCount == 0
      assert httpClient.getHttpClientStats().totalConnectionCount == 0
    }
  }

}
