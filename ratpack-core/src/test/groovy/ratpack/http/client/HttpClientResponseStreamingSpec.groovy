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
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.Blocking
import ratpack.exec.util.ParallelBatch
import ratpack.test.exec.ExecHarness

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
    def p = (1..5000).collect { http.get(applicationUnderTest.address) }
    def b = ParallelBatch.of(p)

    ExecHarness.runSingle {
      b.forEach { i, v -> }.then()
    }
  }
}
