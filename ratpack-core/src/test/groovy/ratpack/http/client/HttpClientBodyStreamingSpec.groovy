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
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import ratpack.exec.Promise
import ratpack.file.FileIo
import ratpack.http.ConnectionClosedException
import ratpack.http.Status
import ratpack.stream.Streams
import spock.util.concurrent.BlockingVariable

import java.nio.file.StandardOpenOption

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
            def responseStream = response.body.bindExec()
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
    def closed = false
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 1 : 0) })
    }
    otherApp {
      post {
        closed = false
        FileIo.write(request.getBodyStream(declaredSize), FileIo.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
          .onError(ConnectionClosedException) {
            closed = true
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
    closed

    and:
    outFile.text == inFile.text

    then:
    text == "java.lang.IllegalStateException: Publisher completed before sending advertised number of bytes"
    closed

    and:
    outFile.text == inFile.text

    where:
    pooled << [true, false]
  }

}
