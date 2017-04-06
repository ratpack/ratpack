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

package ratpack.http.timeout

import io.netty.buffer.Unpooled
import io.netty.handler.codec.PrematureChannelClosureException
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.concurrent.GenericFutureListener
import ratpack.exec.Execution
import ratpack.http.ConnectionClosedException
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable

import java.time.Duration
import java.time.Instant

class HttpServerIdleTimeoutSpec extends RatpackGroovyDslSpec {

  Socket socket() {
    Socket socket = new Socket()
    socket.connect(new InetSocketAddress(address.host, address.port))
    socket
  }

  def "default is no timeout"() {
    when:
    handlers {
      get {
        def has = directChannelAccess.channel.pipeline().get(IdleStateHandler) != null
        render has.toString()
      }
    }

    then:
    text == "false"
  }

  def "times out on read"() {
    when:
    def error = new BlockingVariable<Throwable>(30)
    serverConfig {
      idleTimeout Duration.ofSeconds(1)
    }
    handlers {
      all {
        request.body.map { it.text }
          .onError { error.set(it); context.error(it) }
          .then { response.send(it) }
      }
    }

    and:
    Socket socket = socket()
    def e = new OutputStreamWriter(socket.outputStream, "UTF-8").with {
      write("POST / HTTP/1.1\r\n")
      write("Content-Length: 4000\r\n")
      write("\r\n")
      flush()
      error.get()
    }

    then:
    e instanceof ConnectionClosedException
    e.message.contains("due to idle timeout")

    cleanup:
    socket?.close()
  }

  def "times out on write"() {
    when:
    def closed = new BlockingVariable<Boolean>(30)
    def n = 1024 * 1024 * 10
    GenericFutureListener l = {
      closed.set(true)
    }
    serverConfig {
      idleTimeout Duration.ofSeconds(1)
    }
    handlers {
      get {
        directChannelAccess.channel.closeFuture().addListener(l)
        onClose {
          directChannelAccess.channel.closeFuture().removeListener(l)
        }
        render "a" * n
      }
    }

    and:
    Socket socket = socket()
    new OutputStreamWriter(socket.outputStream, "UTF-8").with {
      write("GET / HTTP/1.1\r\n\r\n")
      flush()
      closed.get()
    }

    then:
    // This is the only way to determine if the connection was _actually_ closed for the client.
    try {
      new OutputStreamWriter(socket.outputStream, "UTF-8").with {
        write("G")
        flush()
        sleep 100
        write("ET / HTTP/1.1\r\n\r\n")
        flush()
      }
      assert false
    } catch (IOException e) {
      assertBrokenSocket(e)
    }
  }

  def "time out on stream write"() {
    when:
    def closed = new BlockingVariable<Boolean>(30)
    serverConfig {
      idleTimeout Duration.ofSeconds(1)
    }
    handlers {
      get {
        def stream = Streams.periodically(context, Duration.ofSeconds(5), { Unpooled.wrappedBuffer("a".bytes) })
          .wiretap {
          if (it.cancel) {
            closed.set(true)
          }
        }

        header("Content-Length", "10")
        response.sendStream(stream)
      }
    }
    getText()

    then:
    thrown PrematureChannelClosureException
    closed.get()
  }

  def "can override idle timeout for request on stream write"() {
    when:
    serverConfig {
      idleTimeout Duration.ofSeconds(1)
    }
    handlers {
      get {
        request.idleTimeout = Duration.ofSeconds(10)
        def stream = Streams.periodically(context, Duration.ofSeconds(3), { it < 2 ? Unpooled.wrappedBuffer("a".bytes) : null })
        header("content-length", "2")
        response.sendStream(stream)
      }
    }

    then:
    text == "aa"
  }

  def "idle timeout is reset after request override"() {
    when:
    def override = Duration.ofSeconds(10)
    serverConfig {
      idleTimeout Duration.ofSeconds(1)
    }
    handlers {
      get("1") {
        request.idleTimeout = override
        render "done"
      }
      get("2") {
        Execution.sleep(override + Duration.ofSeconds(10)).then {
          render "done"
        }
      }
    }
    Socket socket = socket()
    new OutputStreamWriter(socket.outputStream, "UTF-8").with {
      write("GET /1 HTTP/1.1\r\n\r\n")
      flush()
    }

    then:
    def buffer = new StringBuffer()
    while (!buffer.toString().endsWith("done")) {
      buffer.append(socket.inputStream.read() as char)
    }

    when:
    new OutputStreamWriter(socket.outputStream, "UTF-8").with {
      write("GET /2 HTTP/1.1\r\n\r\n")
      flush()
    }

    def start = Instant.now()
    while (socket.inputStream.read() != -1) {
    }

    then:
    Duration.between(start, Instant.now()) < override - Duration.ofSeconds(2)
  }

  boolean assertBrokenSocket(IOException e) {
    assert e.message.contains("Broken pipe") || e.message.contains("Connection reset")
  }

}
