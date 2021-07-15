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
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable

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
}
