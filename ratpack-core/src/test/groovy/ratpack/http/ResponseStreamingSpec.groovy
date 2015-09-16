/*
 * Copyright 2014 the original author or authors.
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
import io.netty.util.CharsetUtil
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Ignore

import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

import static ratpack.stream.Streams.publish

class ResponseStreamingSpec extends RatpackGroovyDslSpec {

  @Ignore
  // unstable due to different buffer sizes on different platforms
  def "can stream response with back pressure"() {
    given:
    def sent = new LinkedBlockingQueue()
    def complete = new CountDownLatch(1)
    Runnable valve
    byte[] bytes

    when:
    handlers {
      get {
        def stream = publish([1, 2, 3, 4, 5]).wiretap {
          if (it.data) {
            sent.put(it.item)
          } else if (it.complete) {
            complete.countDown()
          }
        }.map {
          Unpooled.wrappedBuffer(bytes)
        }.gate {
          valve = it
        }

        response.sendStream(stream)
      }
    }

    Socket socket = new Socket()
    socket.receiveBufferSize = socket.sendBufferSize
    assert socket.receiveBufferSize == socket.sendBufferSize

    bytes = new byte[socket.receiveBufferSize * 2]
    def bufferLength = bytes.length

    socket.connect(new InetSocketAddress(address.host, address.port))
    new OutputStreamWriter(socket.outputStream, "UTF-8").with {
      write("GET / HTTP/1.1\r\n")
      write("Connection: close\r\n")
      write("\r\n")
      flush()
    }

    def is = socket.inputStream
    def reader = new InputStreamReader(is, CharsetUtil.UTF_8)

    reader.readLine() // status line
    reader.readLine() // header/body separating empty line

    then:
    sent.empty

    when:
    valve.run() // let request flow up

    then:
    sent.take() == 1 // first sent directly out and buffered at client
    sent.take() == 2 // second requested by netty and queue there
    sent.empty

    read(is, bufferLength) // consume first from client buffer, transmitting second, creates request for third

    sent.take() == 3
    sent.empty

    read(is, bufferLength)
    read(is, bufferLength)

    sent.take() == 4
    sent.take() == 5
    sent.empty

    read(is, bufferLength)
    read(is, bufferLength)

    sent.empty
    complete.await()

    cleanup:
    socket.close()
  }

  void read(InputStream inputStream, int read) {
    while (read > 0) {
      def bytesRead = inputStream.skip(read)
      if (bytesRead <= 0) {
        throw new IllegalStateException("stream finished early")
      }
      read -= bytesRead
    }
  }
}
