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

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.Blocking
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.exec.util.ParallelBatch
import ratpack.http.ResponseChunks
import ratpack.stream.Streams
import ratpack.stream.bytebuf.ByteBufStreams
import ratpack.test.exec.ExecHarness

import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class HttpClientResponseStreamingSpec extends BaseHttpClientSpec {

  def "can not read streamed request body"() {
    when:
    otherApp {
      get {
        render "1" * 1024
      }
    }

    handlers {
      get { HttpClient http ->
        http.requestStream(otherAppUrl()) {}.then {
          Blocking.op { sleep 1000 }.then { render "ok" }
        }
      }
    }

    then:
    text == "ok"
  }

  def "timeout before headers are received causes promise for streamed response to error"() {
    when:
    otherApp {
      get {
        Execution.sleep(Duration.ofSeconds(2)).then { render "ok" }
      }
    }

    handlers {
      get { HttpClient http ->
        http.requestStream(otherAppUrl()) { it.readTimeout(Duration.ofMillis(500)) }.
          onError { render "requestStream: ${it.class.name}" }.
          then {
            it.body.toList().
              onError { render "body: $it.message" }.
              then { it*.release(); render "ok" }
          }
      }
    }

    then:
    text == "requestStream: $HttpClientReadTimeoutException.name"
  }

  def "timeout after headers are received causes error in body stream"() {
    when:
    otherApp {
      get {
        render ResponseChunks.stringChunks(Streams.flatYield {
          if (it.requestNum < 100) {
            Promise.value("a")
          } else {
            Execution.sleep(Duration.ofSeconds(2)).map { null }
          }
        })
      }
    }

    handlers {
      get { HttpClient http ->
        http.requestStream(otherAppUrl()) { it.readTimeout(Duration.ofMillis(500)) }.
          onError { render "requestStream: ${it.class.name}" }.
          then {
            it.body.reduce(0, { a, b -> b.release(); a }).
              onError { render "body: ${it.class.name}" }.
              then { render "ok" }
          }
      }
    }

    then:
    text == "body: $HttpClientReadTimeoutException.name"
  }

  def "can cancel subscription in complete signal"() {
    when:
    otherApp {
      get {
        response.send()
      }
    }
    handlers {
      get { HttpClient http ->
        def ctx = context
        http.requestStream(otherAppUrl()) {}.then {
          it.body.bindExec().subscribe(new Subscriber<ByteBuf>() {
            Subscription s

            @Override
            void onSubscribe(Subscription s) {
              this.s = s
              s.request(Long.MAX_VALUE)
            }

            @Override
            void onNext(ByteBuf byteBuf) {
              byteBuf.release()
            }

            @Override
            void onError(Throwable t) {
              throw t
            }

            @Override
            void onComplete() {
              s.cancel()
              ctx.render "ok"
            }
          })
        }
      }
    }

    then:
    text == "ok"
  }

  def "chunked responses are streamed reliably"() {
    given:
    def payload = new byte[15 * 8012]
    new Random().nextBytes(payload)

    when:
    otherApp {
      get {
        response.send(payload)
      }
    }
    handlers {
      get { HttpClient http ->
        http.requestStream(otherAppUrl(), {}).then { StreamedResponse streamedResponse ->
          streamedResponse.forwardTo(response)
        }
      }
    }

    then:
    (1..10).collect { get().body.bytes == payload }.every()
  }

  def "can safely stream empty body"() {
    when:
    otherApp {
      get {
        response.status(204).send()
      }
    }
    handlers { HttpClient httpClient ->
      get {
        httpClient.requestStream(otherAppUrl(), {}).then {
          it.forwardTo(context.response)
        }
      }
    }
    def http = HttpClient.of { it.poolSize(30) }

    then:
    def p = (1..100).collect { http.get(applicationUnderTest.address) }
    def b = ParallelBatch.of(p)

    ExecHarness.runSingle {
      b.forEach { i, v -> }.then()
    }
  }

  def "connection is closed if response is not read"() {
    when:
    Channel channel
    otherApp {
      get {
        channel = directChannelAccess.channel
        render "foo"
      }
    }

    handlers { HttpClient httpClient ->
      get {
        httpClient.requestStream(otherAppUrl(), {}).then {
          render "ok"
        }
      }
    }

    then:
    text == "ok"
    channel.closeFuture().sync()
  }

  def "keep alive connection is closed if response is not read"() {
    when:
    Channel channel
    otherApp {
      get {
        channel = directChannelAccess.channel
        render "foo" * 8192 * 100 // must be bigger than one buffer, otherwise may be read implicitly.
      }
    }

    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(8) })
    }

    handlers { HttpClient httpClient ->
      get {
        httpClient.requestStream(otherAppUrl(), {}).then {
          render "ok"
        }
      }
    }

    then:
    text == "ok"
    channel.closeFuture().sync()
  }

  def "keep alive connection is not closed if response is not read but had no body"() {
    when:
    Channel channel
    otherApp {
      get {
        channel = directChannelAccess.channel
        response.send()
      }
    }

    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(8) })
    }

    handlers { HttpClient httpClient ->
      get {
        httpClient.requestStream(otherAppUrl(), {}).then {
          render "ok"
        }
      }
    }

    then:
    text == "ok"

    when:
    channel.closeFuture().get(2, TimeUnit.SECONDS)

    then:
    thrown TimeoutException
  }

  def "keep alive connection is not closed if response is not read but has no body"() {
    when:
    Channel channel
    otherApp {
      get {
        if (channel == null) {
          channel = directChannelAccess.channel
        } else {
          assert channel == directChannelAccess.channel
        }
        response.send()
      }
    }

    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(8) })
    }

    handlers { HttpClient httpClient ->
      get {
        httpClient.requestStream(otherAppUrl(), {}).then {
          render "ok"
        }
      }
    }

    then:
    text == "ok"
    text == "ok"
  }

  def "read timeout is propagated when reading response stream"() {
    when:
    otherApp {
      get {
        render ResponseChunks.stringChunks(Streams.periodically(context, Duration.ofSeconds(5), { "a" }))
      }
    }
    handlers {
      get {
        get(HttpClient).requestStream(otherApp.address) {
          it.readTimeout(Duration.ofSeconds(2))
        }.then {
          ByteBufStreams.compose(it.body)
            .onError(HttpClientReadTimeoutException) { render "timeout" }
            .then { response.send it }
        }
      }
    }

    then:
    text == "timeout"
  }

  def "can influence chunk size"() {
    when:
    def s = "a" * 102400
    int max = 0
    otherApp {
      get {
        render s
      }
    }
    handlers {
      def http = HttpClient.of {
        if (c) {
          it.responseMaxChunkSize(c)
        }
      }

      get {
        http.requestStream(otherAppUrl(), {
          if (r) {
            it.responseMaxChunkSize(r)
          }
        }).then {
          render ResponseChunks.bufferChunks("text/plain", it.getBody().wiretap {
            if (it.data) {
              max = Math.max(max, it.item.readableBytes())
            }
          })
        }
      }
    }

    then:
    text == s
    max == size

    where:
    c    | r     | size
    0    | 0     | 8192
    1024 | 0     | 1024
    1024 | 2048  | 2048
    0    | 10240 | 10240
  }
}
