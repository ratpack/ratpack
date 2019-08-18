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
import ratpack.exec.Execution
import ratpack.http.client.HttpClient
import ratpack.stream.StreamEvent
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

import static ratpack.http.ResponseChunks.stringChunks
import static ratpack.stream.Streams.publish

class ResponseStreamingSpec extends RatpackGroovyDslSpec {

  def "too large unread request body is silently discarded when streaming a response"() {
    when:
    handlers {
      all {
        request.maxContentLength = 12
        render stringChunks(
          publish(["abc"] * 3)
        )
      }
    }

    then:
    def r = request { it.post().body.text("a" * 100_000) }
    r.status.code == 200
    r.body.text == "abc" * 3
  }

  def "unread request body is silently discarded when streaming a response"() {
    when:
    handlers {
      all {
        request.maxContentLength = 100_000
        render stringChunks(
          publish(["abc"] * 3)
        )
      }
    }

    then:
    def r = request { it.post().body.text("a" * 1000) }
    r.status.code == 200
    r.body.text == "abc" * 3
  }

  def "client cancellation causes server publisher to receive cancel"() {
    given:
    Queue<StreamEvent<ByteBuf>> events = new ConcurrentLinkedQueue<>()
    def buffer = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("a" * 30720, Charsets.UTF_8))

    when:
    handlers {
      get {
        def stream = Streams.constant(buffer).wiretap {
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
              Execution.sleep(Duration.ofSeconds(1)).then {
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

    cleanup:
    buffer.release()
  }

  def "client cancellation before reading headers causes stream to cancel"() {
    given:
    Queue<StreamEvent<ByteBuf>> events = new ConcurrentLinkedQueue<>()
    def buffer = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("a" * 10, Charsets.UTF_8))

    when:
    handlers {
      get {
        def stream = Streams.periodically(context, Duration.ofSeconds(10), { buffer }).wiretap {
          events << it
        }
        response.sendStream(stream.bindExec())
      }
    }

    then:
    InputStream is = application.address.toURL().openStream()
    Thread.sleep(1000)
    is.close()
    new PollingConditions(timeout: 2).eventually {
      events.find { it.cancel }
    }

    cleanup:
    buffer.release()
  }

  def "request outcome has duration if client disconnects before sending response"() {
    when:
    def sentAt = new BlockingVariable<Instant>(5)
    handlers {
      get {
        onClose {
          sentAt.set(it.sentAt)
        }
        Execution.sleep(Duration.ofMillis(1000)).then {
          render stringChunks("text/plain", Streams.constant("a"))
        }
      }
    }

    def socket = socket()
    new OutputStreamWriter(socket.outputStream, "UTF-8").with {
      write("GET / HTTP/1.1\r\n")
      write("\r\n")
      flush()
    }
    socket.close()

    then:
    sentAt.get() != null
  }

  def "can add response finalizer to streamed response"() {
    when:
    handlers {
      all {
        response.beforeSend {
          it.headers.add("foo", "1")
          response.beforeSend {
            it.headers.add("bar", "1")
          }
        }
        request.maxContentLength = 12
        render stringChunks(
          publish(["abc"] * 3)
        )
      }
    }

    then:
    def r = request { it.post().body.text("a" * 100_000) }
    r.status.code == 200
    r.body.text == "abc" * 3
    r.headers.foo == "1"
    r.headers.bar == "1"
  }
}
