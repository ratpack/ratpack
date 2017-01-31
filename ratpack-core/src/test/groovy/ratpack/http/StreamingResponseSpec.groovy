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

package ratpack.http

import com.google.common.base.Charsets
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.Promise
import ratpack.http.client.HttpClient
import ratpack.stream.StreamEvent
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ConcurrentLinkedQueue

class StreamingResponseSpec extends RatpackGroovyDslSpec {

  def "client cancellation causes server publisher to receive cancel"() {
    given:
    Queue<StreamEvent<ByteBuf>> events = new ConcurrentLinkedQueue<>()

    when:
    handlers {
      get {
        def stream = Streams.constant(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("a" * 1024 * 10, Charsets.UTF_8)))
          .wiretap {
          events << it
        }

        response.sendStream(stream)
      }
      get("read") { ctx ->
        get(HttpClient).requestStream(application.address) {

        }.then {
          it.body.subscribe(new Subscriber<ByteBuf>() {
            @Override
            void onSubscribe(Subscription s) {
              Promise.value(null).defer { r ->
                Thread.start {
                  sleep 3000
                  r.run()
                }
              }.then {
                s.cancel()
                ctx.render "cancel"
              }
            }

            @Override
            void onNext(ByteBuf byteBuf) {

            }

            @Override
            void onError(Throwable t) {

            }

            @Override
            void onComplete() {

            }
          })
        }
      }
    }

    then:
    getText("read") == "cancel"
    new PollingConditions().eventually {
      events.find { it.cancel }
    }
  }

}
