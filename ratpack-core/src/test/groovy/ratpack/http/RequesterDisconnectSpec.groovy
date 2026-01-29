/*
 * Copyright 2021 the original author or authors.
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

import io.netty.buffer.Unpooled
import ratpack.exec.Promise
import ratpack.sse.ServerSentEvents
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable

import java.util.concurrent.CountDownLatch

class RequesterDisconnectSpec extends RatpackGroovyDslSpec {

  def "gracefully handles disconnect after sending request"() {
    given:
    def connectionClosed = new BlockingVariable(10)
    handlers {
      get {
        Promise.async { down ->
          directChannelAccess.channel.closeFuture().addListener { down.success(true) }
        } then {
          connectionClosed.set(true)
          render "closed"
        }
      }
    }

    when:
    def socket = socket()
    withSocket(socket) {
      write("GET / HTTP/1.1\r\n")
      write("\r\n")
      flush()
    }
    socket.close()

    then:
    connectionClosed.get()
  }

  def "gracefully handles disconnect while sending request body"() {
    given:
    def connectionClosed = new BlockingVariable(10)
    handlers {
      post {
        request.body
          .onError(ConnectionClosedException) {
            connectionClosed.set(true)
            render "error"
          }
          .then {
            connectionClosed.set(false)
            render "closed"
          }
      }
    }

    when:
    def socket = socket()
    withSocket(socket) {
      write("POST / HTTP/1.1\r\n")
      write("content-length: 10\r\n")
      write("\r\n")
      write("abcd")
      flush()
    }
    socket.close()

    then:
    connectionClosed.get()
  }

  def "gracefully handles disconnect while writing response headers"() {
    given:
    def executionClosed = new BlockingVariable(10)
    def responseBodyChunk = Unpooled.wrappedBuffer(("a" * 1024).bytes)
    def responseBody = Unpooled.compositeBuffer(256)
    256.times {
      responseBody.addComponent(true, responseBodyChunk.retainedSlice())
    }
    handlers {
      post {
        context.onClose { executionClosed.set(true) }
        request.body
          .then {
            response.send(responseBody)
          }
      }
    }

    when:
    def socket = socket()
    withSocket(socket) {
      write("POST / HTTP/1.1\r\n")
      write("content-length: 10\r\n")
      write("\r\n")
      write("abcdefghij")
      flush()
    }
    "HTTP/1.1 200 OK".size().times {
      socket.inputStream.read() as char
    }
    socket.close()

    then:
    executionClosed.get()

    cleanup:
    responseBodyChunk.release()
  }

  def "gracefully handles disconnect while streaming large SSE response"() {
    given:
    def executionClosed = new BlockingVariable(10)
    def firstEventSent = new CountDownLatch(1)

    handlers {
      get {
        context.onClose { executionClosed.set(true) }

        // Create a large SSE stream with big events to fill the buffer quickly
        def stream = Streams.publish(1..1000).map { i ->
          if (i == 1) {
            firstEventSent.countDown()
          }
          // Each event is ~1KB to quickly fill the network buffer
          ratpack.sse.ServerSentEvent.builder()
            .id(i.toString())
            .event("data")
            .data(("x" * 1024).toString())
            .build()
        }

        render ServerSentEvents.builder().build(stream)
      }
    }

    when:
    def socket = socket()
    withSocket(socket) {
      write("GET / HTTP/1.1\r\n")
      write("\r\n")
      flush()
    }

    // Wait for first event to be sent (means streaming has started)
    firstEventSent.await()

    // Read a bit of the response to ensure we're mid-stream
    20.times {
      socket.inputStream.read()
    }

    // Close connection while streaming
    socket.close()

    then:
    executionClosed.get()
  }

  def "gracefully handles disconnect while streaming with small buffer"() {
    given:
    def executionClosed = new BlockingVariable(10)
    def firstChunkSent = new CountDownLatch(1)

    handlers {
      get {
        context.onClose {
          executionClosed.set(true)
        }

        // Stream with many small chunks to increase chance of hitting the race condition
        def stream = Streams.publish(1..500).map { i ->
          if (i == 1) {
            firstChunkSent.countDown()
          }
          Unpooled.wrappedBuffer(("chunk-$i-" + ("y" * 100)).bytes)
        }

        response.sendStream(stream)
      }
    }

    when:
    def socket = socket()
    withSocket(socket) {
      write("GET / HTTP/1.1\r\n")
      write("\r\n")
      flush()
    }

    // Wait for first chunk
    firstChunkSent.await()

    // Read some bytes to ensure we're streaming
    10.times {
      socket.inputStream.read()
    }

    // Close connection abruptly
    socket.close()

    then:
    executionClosed.get()
  }

  def "gracefully handles disconnect with very large streaming response"() {
    given:
    def executionClosed = new BlockingVariable(10)
    def streamStarted = new CountDownLatch(1)

    // Create a massive response to ensure buffer fills up
    def responseBodyChunk = Unpooled.wrappedBuffer(("a" * 1024).bytes)
    def responseBody = Unpooled.compositeBuffer(2048) // 2MB
    2048.times {
      responseBody.addComponent(true, responseBodyChunk.retainedSlice())
    }

    handlers {
      post {
        context.onClose { executionClosed.set(true) }
        request.body.then {
          streamStarted.countDown()
          response.send(responseBody)
        }
      }
    }

    when:
    def socket = socket()
    withSocket(socket) {
      write("POST / HTTP/1.1\r\n")
      write("content-length: 5\r\n")
      write("\r\n")
      write("hello")
      flush()
    }

    streamStarted.await()

    // Read just the headers, then close
    def line = new StringBuilder()
    while (true) {
      int b = socket.inputStream.read()
      if (b == -1) {
        break
      }
      line.append((char) b)
      if (line.toString().endsWith("\r\n\r\n")) {
        break
      }
    }

    socket.close()

    then:
    executionClosed.get()

    cleanup:
    responseBodyChunk.release()
  }
}
