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

package ratpack.http.client.internal

import io.netty.buffer.Unpooled
import io.netty.channel.ConnectTimeoutException
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.util.CharsetUtil
import org.spockframework.compiler.model.WhereBlock
import ratpack.exec.Blocking
import ratpack.http.client.HttpClient
import ratpack.http.client.HttpClientSpec
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.http.client.StreamedResponse
import ratpack.stream.Streams
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Unroll

import java.time.Duration
import java.util.zip.GZIPInputStream

import static ratpack.http.ResponseChunks.stringChunks
import static ratpack.http.internal.HttpHeaderConstants.CONTENT_ENCODING
import static ratpack.sse.ServerSentEvents.serverSentEvents
import static ratpack.stream.Streams.publish

@Unroll
class PooledHttpClientSmokeSpec extends HttpClientSpec implements PooledHttpClientFactory {

  def "can make simple get request"() {
    given:
    otherApp {
      get("foo") {
        render "bar"
      }
    }

    when:
    handlers {
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
    otherApp {
      post("foo") {
        request.body.then { body ->
          render body.text
        }
      }
    }

    when:
    handlers {
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
    otherApp {
      get {
        render "foo"
      }
    }

    when:
    handlers {
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
    otherApp {
      post {
        request.body.then { body ->
          render body.text
        }
      }
    }

    when:
    handlers {
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
    otherApp {
      post {
        request.body.then { body ->
          render body.text
        }
      }
    }

    when:
    handlers {
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
    otherApp {
      post {
        request.body.then { body ->
          render body.contentType.toString()
        }
      }
    }

    when:
    handlers {
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
    otherApp {}

    and:
    handlers {
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
  def "can set connect timeout"() {
    setup:
    def nonRoutableIp = '192.168.0.0'
    def pooledHttpConfig = new PooledHttpConfig(connectionTimeoutMillis: 20)

    when:
    handlers {
      get {
        HttpClient httpClient = createClient(context, pooledHttpConfig)
        httpClient.get("http://$nonRoutableIp".toURI()).onError {
          render it.class.name
        } then {
          render "success"
        }
      }
    }

    then:
    text == ConnectTimeoutException.name

    where:
    pooled << [true, false]
  }

  def "can set read timeout"() {
    setup:
    def pooledHttpConfig = new PooledHttpConfig(readTimeoutMillis: 1)

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
      get {
        HttpClient httpClient = createClient(context, pooledHttpConfig)
        httpClient.get(otherAppUrl()).onError {
          render it.class.name
        } then {
          render "success"
        }
      }
    }

    then:
    text == ReadTimeoutException.name

    where:
    pooled << [true, false]
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
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
        httpClient.requestStream(otherAppUrl("foo")) {
        } then { StreamedResponse responseStream ->
          responseStream.forwardTo(response)
        }
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
connection: keep-alive
transfer-encoding: chunked

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
    otherApp {
      get("foo") {
        render stringChunks(
          publish(["bar"] * 3)
        )
      }
    }

    and:
    handlers {
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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

    where:
    pooled << [true, false]
  }

  //TODO - Random test case failure ... Need to dig into root cause.
  @Ignore
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
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
    def pooledHttpConfig = new PooledHttpConfig(decompressResponse: false)
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
      get {
        HttpClient httpClient = createClient(context, pooledHttpConfig)
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
      get {
        HttpClient httpClient = createClient(context, new PooledHttpConfig(pooled: pooled))
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
    def pooledHttpConfig = new PooledHttpConfig(decompressResponse: false, pooled: pooled)
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
      get {
        HttpClient httpClient = createClient(context, pooledHttpConfig)
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

  @Unroll
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
      spec.method("OPTIONS")
    }
    def pathResponse = request("foo") { spec ->
      spec.method("OPTIONS")
    }

    then:
    response.status.code == 200
    response.headers.get("ALLOW") == ["GET", "PUT", "POST", "DELETE", "PATCH"].join(",")

    and:
    pathResponse.status.code == 200
    pathResponse.headers.get("ALLOW") == ["GET"].join(",")
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
      spec.method("HEAD")
    }
    def pathResponse = request("foo") { spec ->
      spec.method("HEAD")
    }

    then:
    response.status.code == 200

    and:
    pathResponse.status.code == 404
  }

}
