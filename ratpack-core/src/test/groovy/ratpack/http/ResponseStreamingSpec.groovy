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
import io.netty.handler.codec.PrematureChannelClosureException
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.ExecInterceptor
import ratpack.exec.Execution
import ratpack.func.Block
import ratpack.http.client.HttpClient
import ratpack.http.internal.DefaultResponse
import ratpack.stream.StreamEvent
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable
import spock.util.concurrent.PollingConditions

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

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
    r.status == Status.PAYLOAD_TOO_LARGE
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
    def onCloseCalled = new BlockingVariable()

    when:
    handlers {
      get {
        def stream = Streams.constant(buffer).wiretap {
          events << it
        }
        context.onClose { onCloseCalled.set(true) }
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
    onCloseCalled.get()

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
        request.maxContentLength = 100_000
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

  def "double transmission while streaming closes response and cancels stream"() {
    when:
    def cancelled = new BlockingVariable()
    handlers {
      all {
        response.sendStream(new Publisher<ByteBuf>() {
          @Override
          void subscribe(Subscriber<? super ByteBuf> s) {
            s.onSubscribe(new Subscription() {
              @Override
              void request(long n) {
                throw new RuntimeException("!!")
              }

              @Override
              void cancel() {
                cancelled.set(true)
              }
            })
          }
        })
      }
    }

    then:
    def r = get()
    r.status.code == 200
    r.body.text == ""
    cancelled
  }

  def "double transmission before streaming closes response and cancels stream"() {
    when:
    def cancelled = new BlockingVariable()
    handlers {
      all {
        response.sendStream(new Publisher<ByteBuf>() {
          @Override
          void subscribe(Subscriber<? super ByteBuf> s) {
            s.onSubscribe(new Subscription() {
              @Override
              void request(long n) {
                s.onComplete()
              }

              @Override
              void cancel() {
                cancelled.set(true)
              }
            })
          }
        })
        throw new RuntimeException("!!")
      }
    }

    and:
    get()

    then:
    thrown PrematureChannelClosureException
  }

  def "can stream response concurrently with sending body"() {
    when:
    serverConfig {
      threads(1)
      maxChunkSize(1)
    }
    handlers {
      all { ctx ->
        (response as DefaultResponse)
          .sendStream(request.bodyStream.map {
            Unpooled.compositeBuffer(2)
              .addComponent(true, it)
              .addComponent(true, Unpooled.wrappedBuffer("\n".bytes))
          }, false)
      }
    }

    and:
    def socket = socket()
    def input = new BufferedReader(new InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
    def output = new OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8)
    def pieces = 100
    output.with {
      write("POST / HTTP/1.1\r\n")
      write("Content-Length: ${pieces}\r\n")
      write("\r\n")
      flush()
    }

    then:
    input.readLine() == "HTTP/1.1 200 OK"
    input.readLine() == "transfer-encoding: chunked"
    input.readLine() == ""

    and:
    pieces.times {
      output.with { write("a"); flush() }
      assert input.readLine() == "2"
      assert input.readLine() == "a"
      assert input.readLine() == ""
    }
    input.readLine() == "0"

    cleanup:
    output?.close()
    input?.close()
  }

  def "stream response close listener is in context of execution"() {
    when:
    def array = new byte[1024 * 1024]
    Arrays.fill(array, 1 as byte)
    def data = Unpooled.wrappedBuffer(array)
    def exec = new AtomicReference<Execution>()
    def execSuspended = new BlockingVariable<Boolean>()
    def execSet = new BlockingVariable<Boolean>()
    serverConfig {
      maxChunkSize(array.size())
    }
    bindings {
      bindInstance(ExecInterceptor, new ExecInterceptor() {
        @Override
        void intercept(Execution execution, ExecInterceptor.ExecType execType, Block executionSegment) throws Exception {
          try {
            executionSegment.execute()
          } finally {
            if (execution == exec.get() && execType == ExecInterceptor.ExecType.COMPUTE) {
              execSuspended.set(true)
            }
          }
        }
      })
    }
    handlers {
      all { ctx ->
        exec.set(Execution.current())

        context.onClose {
          execSet.set(Execution.current().is(exec.get()))
        }

        response.sendStream(Streams.yield { data.retainedSlice() })
      }
    }

    and:
    def socket = socket()
    def input = new BufferedReader(new InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
    def output = new OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8)
    output.with {
      write("POST / HTTP/1.1\r\n")
      write("\r\n")
      flush()
    }

    then:
    input.readLine() == "HTTP/1.1 200 OK"
    input.readLine() == "transfer-encoding: chunked"
    input.readLine() == ""

    and:
    10.times {
      input.readLine()
      input.readLine()
    }

    when:
    execSuspended.get()
    input.close()

    then:
    execSet.get()

    cleanup:
    data?.release()
    output?.close()
    input?.close()
  }

}
