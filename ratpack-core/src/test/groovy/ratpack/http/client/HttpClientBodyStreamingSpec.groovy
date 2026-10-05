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

import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.channel.EventLoop
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.file.FileIo
import ratpack.http.ConnectionClosedException
import ratpack.http.Status
import ratpack.stream.Streams
import spock.util.concurrent.BlockingVariable

import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.util.zip.GZIPOutputStream

class HttpClientBodyStreamingSpec extends BaseHttpClientSpec {

  def "can stream file"() {
    given:
    def size = 1024 * 1024 * 3
    def inFile = baseDir.write("in", "a" * size)
    def outFile = baseDir.path("out")
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        FileIo.write(request.getBodyStream(size), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
          .then { render "out" }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.request(otherAppUrl()) {
          def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
            .bindExec()
          it.post().body.stream(stream, size)
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    where:
    pooled << [true, false]
  }


  def "client can be used to stream response as request"() {
    given:
    def size = 1024 * 1024 * 10
    def inFile = baseDir.write("in", "a" * size)
    def outFile = baseDir.path("out")
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 2 : 0) })
    }
    otherApp {
      all {
        byMethod {
          get {
            render inFile
          }
          post {
            FileIo.write(request.getBodyStream(size), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
              .then { render "out" }
          }
        }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.requestStream(otherAppUrl()) {

        } then { StreamedResponse response ->
          render(Promise.flatten {
            def responseStream = response.body.bindExec { it.release() }
            httpClient.request(otherAppUrl()) { it.post().maxContentLength(size).body.stream(responseStream, size) }
              .map { it.body.text }
          }.fork())
        }
      }
    }

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    where:
    pooled << [true, false]
  }


  def "can stream body from publisher that emits beyond write buffer high water mark when first requested"() {
    given:
    // More than the default 64KB write buffer high water mark, emitted synchronously on request,
    // so the channel turns unwritable (and, on loopback, writable again) before onSubscribe returns.
    def chunk = ("a" * 8192).bytes
    def numChunks = 32
    def size = chunk.length * numChunks
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        render request.getBodyStream(size).reduce(0L) { total, received ->
          def readable = received.readableBytes()
          received.release()
          total + readable
        }.map { it.toString() }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = Streams.publish((1..numChunks).collect { Unpooled.wrappedBuffer(chunk) })
        render httpClient.request(otherAppUrl()) { it.post().maxContentLength(size).body.stream(stream, size) }
          .map { it.body.text }
      }
    }

    then:
    text == size.toString()
    text == size.toString()

    where:
    pooled << [true, false]
  }

  def "client can send unknown length"() {
    given:
    def size = 1024 * 1024 * 3
    def inFile = baseDir.write("in", "a" * size)
    def outFile = baseDir.path("out")
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        FileIo.write(request.getBodyStream(size), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
          .then {
            render "out"
          }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
          .bindExec()

        render httpClient.request(otherAppUrl()) { it.post().maxContentLength(size).body.streamUnknownLength(stream) }
          .map { it.body.text }
      }
    }

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    where:
    pooled << [true, false]
  }

  def "can cleanly handle early request close with fixed length"() {
    given:
    def size = 1024 * 1024 * 10
    def inFile = baseDir.write("in", "a" * size)
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        request.setMaxContentLength(size - 1)
        render("ok")
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
          .bindExec { it.release() }

        render httpClient.request(otherAppUrl()) { it.post().maxContentLength(size).body.stream(stream, size) }
          .map { it.statusCode.toString() }
      }
    }

    then:
    text == Status.PAYLOAD_TOO_LARGE.code.toString()
    text == Status.PAYLOAD_TOO_LARGE.code.toString()

    where:
    pooled << [true, false]
  }

  def "can redirect fixed length request with acceptable body"() {
    given:
    def size = 1024 * 1024 * 3
    def inFile = baseDir.write("in", "a" * size)
    def outFile = baseDir.path("out")
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        request.setMaxContentLength(size + 1)
        redirect(307, "end")
      }
      post("end") {
        FileIo.write(request.getBodyStream(size), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
          .then { render "out" }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
          .bindExec { it.release() }

        render httpClient.request(otherAppUrl()) { it.post().maxContentLength(size).body.stream(stream, size) }
          .map { it.body.text }
      }
    }

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    where:
    pooled << [true, false]
  }

  def "can cleanly handle early request close with unknown length"() {
    given:
    def size = 1024 * 1024 * 10
    def inFile = baseDir.write("in", "a" * size)
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        request.setMaxContentLength(size / 4 as long)
        render("ok")
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
          .bindExec { it.release() }

        render httpClient.request(otherAppUrl()) { it.post().maxContentLength(size).body.streamUnknownLength(stream) }
          .map { it.statusCode.toString() }
      }
    }

    then:
    text == Status.PAYLOAD_TOO_LARGE.code.toString()
    text == Status.PAYLOAD_TOO_LARGE.code.toString()

    where:
    pooled << [true, false]
  }

  def "can redirect unknown length request with acceptable body"() {
    given:
    def size = 1024 * 1024 * 3
    def inFile = baseDir.write("in", "a" * size)
    def outFile = baseDir.path("out")
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        request.setMaxContentLength(size + 1)
        redirect(307, "end")
      }
      post("end") {
        FileIo.write(request.getBodyStream(size), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
          .then { render "out" }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
          .bindExec { it.release() }

        render httpClient.request(otherAppUrl()) { it.post().maxContentLength(size).body.streamUnknownLength(stream) }
          .map { it.body.text }
      }
    }

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    where:
    pooled << [true, false]
  }


  def "terminates on error with fixed length"() {
    given:
    def size = 4096 * 10
    def closed = new BlockingVariable()
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        render request.getBodyStream(size).reduce("ok") { result, chunk ->
          chunk.release()
          result
        }
          .onError(ConnectionClosedException) {
            closed.set(true)
            render "closed"
          }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = Streams.yield {
          if (it.requestNum == Math.floor(size / 2)) {
            throw new Exception("!")
          } else {
            Unpooled.wrappedBuffer("1".bytes)
          }
        }

        render httpClient.request(otherAppUrl()) {
          it.post().maxContentLength(size).body.stream(stream, size)
        }
          .map { it.body.text }
          .mapError { "error" }
      }
    }

    then:
    text == "error"
    closed.get()
    text == "error"
    closed.get()

    where:
    pooled << [true, false]
  }

  def "terminates on error with unknown length"() {
    given:
    def chunk = ("1" * 4096).bytes
    def numChunks = 10
    def size = chunk.length * numChunks
    def closed = new BlockingVariable()
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        render request.getBodyStream(size).reduce("ok") { result, recChunk ->
          recChunk.release()
          result
        }
          .onError(ConnectionClosedException) {
            closed.set(true)
            render "closed"
          }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = Streams.yield {
          if (it.requestNum == Math.floor(numChunks / 2)) {
            throw new Exception("!")
          } else {
            Unpooled.wrappedBuffer(chunk)
          }
        }

        render httpClient.request(otherAppUrl()) {
          it.post().maxContentLength(size).body.streamUnknownLength(stream)
        }
          .map { it.body.text }
          .mapError { "error" }
      }
    }

    then:
    text == "error"
    closed.get()
    text == "error"
    closed.get()

    where:
    pooled << [true, false]
  }

  def "can redirect fixed length request with too large body when using expect-continue"() {
    given:
    def size = 1024 * 1024 * 10
    def inFile = baseDir.write("in", "a" * size)
    def outFile = baseDir.path("out")
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        request.setMaxContentLength(size - 1)
        redirect(307, "end")
      }
      post("end") {
        FileIo.write(request.getBodyStream(size), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
          .then {
            render "out"
          }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
          .bindExec { it.release() }

        render httpClient.request(otherAppUrl()) {
          it.post().maxContentLength(size)
            .headers { it.set(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE) }
            .body.stream(stream, size)
        }
          .map { it.body.text }
      }
    }

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    where:
    pooled << [true, false]
  }

  def "can redirect unknown length request with too large body when using expect-continue"() {
    given:
    def size = 1024 * 1024 * 10
    def inFile = baseDir.write("in", "a" * size)
    def outFile = baseDir.path("out")
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        request.setMaxContentLength(size - 1)
        redirect(307, "end")
      }
      post("end") {
        FileIo.write(request.getBodyStream(size), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
          .then { render "out" }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
          .bindExec { it.release() }

        render httpClient.request(otherAppUrl()) {
          it.post().maxContentLength(size)
            .headers { it.set(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE) }
            .body.streamUnknownLength(stream)
        }
          .map { it.body.text }
      }
    }

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    then:
    text == "out"

    and:
    inFile.text == outFile.text

    where:
    pooled << [true, false]
  }

  def "sends only declared size"() {
    given:
    def size = 1024 * 1024 * 3
    def declaredSize = (long) (size / 4)
    def inFile = baseDir.write("in", "a" * size)
    def outFile = baseDir.path("out")
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        FileIo.write(request.getBodyStream(size), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
          .then { render "out" }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
          .bindExec { it.release() }

        render httpClient.request(otherAppUrl()) { it.post().maxContentLength(size).body.stream(stream, declaredSize) }
          .map { it.body.text }
      }
    }

    then:
    text == "out"

    and:
    outFile.text == ("a" * declaredSize)

    then:
    text == "out"

    and:
    outFile.text == ("a" * declaredSize)

    where:
    pooled << [true, false]
  }

  def "errors if declared size is larger than actual"() {
    given:
    def size = 1024 * 1024 * 3
    def declaredSize = size + 20
    def inFile = baseDir.write("in", "a" * size)
    def outFile = baseDir.path("out")
    def closed = new BlockingVariable<Boolean>()
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        closed = new BlockingVariable<Boolean>()
        FileIo.write(request.getBodyStream(declaredSize), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
          .onError(ConnectionClosedException) {
            closed.set(true)
            render "closed"
          }
          .then { render "out" }
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def stream = FileIo.readStream(FileIo.open(inFile), PooledByteBufAllocator.DEFAULT, 4096)
          .bindExec { it.release() }

        render httpClient.request(otherAppUrl()) { it.post().maxContentLength(size).body.stream(stream, declaredSize) }
          .map { it.body.text }
          .mapError { it.toString() }
      }
    }

    then:
    text == "java.lang.IllegalStateException: Publisher completed before sending advertised number of bytes"
    closed.get()

    and:
    outFile.size() > 0

    then:
    text == "java.lang.IllegalStateException: Publisher completed before sending advertised number of bytes"
    closed.get()

    and:
    outFile.size() > 0

    where:
    pooled << [true, false]
  }

  def "can consume streamed response body on a different event loop while content is still being received"() {
    given:
    // A gzipped response, where each write the server makes ends with a block that decompresses to nothing.
    // The client's decompressor then keeps the channel reading on its own, so content keeps arriving on the
    // channel's event loop whether or not the body has been subscribed to.
    def lines = (0..<3000).collect { String.format("%07d", it) }
    def writes = gzipWrites(lines.collate(50))
    def serverSocket = new ServerSocket(0, 50, InetAddress.loopbackAddress)
    def serverThread = Thread.start {
      while (!serverSocket.closed) {
        Socket socket
        try {
          socket = serverSocket.accept()
        } catch (SocketException ignore) {
          break
        }
        Thread.start {
          socket.withCloseable {
            int last4 = 0
            while (last4 != 0x0d0a0d0a) {
              int b = socket.inputStream.read()
              if (b < 0) {
                return
              }
              last4 = (last4 << 8) | b
            }
            def head = "HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nTransfer-Encoding: chunked\r\nConnection: close\r\n\r\n"
            socket.outputStream.write(concat(head.bytes, writes.head()))
            socket.outputStream.flush()
            writes.tail().each {
              sleep(1)
              socket.outputStream.write(it)
              socket.outputStream.flush()
            }
          }
        }
      }
    }
    serverConfig {
      threads(2)
    }
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(0) })
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        def requestingEventLoop = Execution.current().eventLoop
        def otherEventLoop = Execution.current().controller.eventLoopGroup.find { it != requestingEventLoop } as EventLoop
        httpClient.requestStream(new URI("http://localhost:${serverSocket.localPort}/")) {
        } then { StreamedResponse response ->
          // Let some content be buffered before subscribing, while more is arriving
          def received = Promise.value(null).defer(Duration.ofMillis(10)).flatMap {
            response.body.bindExec { it.release() }.reduce(new StringBuilder()) { text, buffer ->
              text.append(buffer.toString(StandardCharsets.UTF_8))
              buffer.release()
              text
            }
          }
          render received
            .map {
              def receivedLines = it.toString().readLines()
              def firstDifference = (0..<lines.size()).find { i -> receivedLines[i] != lines[i] }
              firstDifference == null && receivedLines.size() == lines.size() ? "ok" : "received ${receivedLines.size()} lines, first difference at line ${firstDifference}".toString()
            }
            .mapError { it.toString() }
            .fork { it.eventLoop(otherEventLoop) }
        }
      }
    }

    then:
    (1..20).collect { text }.findAll { it != "ok" } == []

    cleanup:
    serverSocket?.close()
    serverThread?.join()
  }

  // Encodes the lines as a chunked gzip body, grouped into separate writes.
  // The first write is just the gzip header, which decompresses to nothing.
  // Every other write is a series of chunks, one separately flushed gzip block per line, ending with an empty
  // stored block that decompresses to nothing.
  private static List<byte[]> gzipWrites(List<List<String>> lineGroups) {
    byte[] emptyStoredBlock = [0, 0, 0, 0xff, 0xff] as byte[]
    def compressed = new ByteArrayOutputStream()
    def gzip = new GZIPOutputStream(compressed, true)
    def chunk = { byte[] bytes ->
      concat("${Integer.toHexString(bytes.length)}\r\n".bytes, bytes, "\r\n".bytes)
    }
    def take = {
      def bytes = compressed.toByteArray()
      compressed.reset()
      chunk(bytes)
    }

    gzip.flush()
    List<byte[]> writes = [take()]
    lineGroups.each { group ->
      def write = new ByteArrayOutputStream()
      group.each { line ->
        gzip.write("$line\n".bytes)
        gzip.flush()
        write.write(take())
      }
      write.write(chunk(emptyStoredBlock))
      writes << write.toByteArray()
    }
    gzip.close()
    writes << concat(take(), chunk(new byte[0]))
    writes
  }

  private static byte[] concat(byte[]... parts) {
    def bytes = new ByteArrayOutputStream()
    parts.each { bytes.write(it) }
    bytes.toByteArray()
  }

}
