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

package ratpack.file

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec

import java.nio.file.StandardOpenOption

import static java.nio.file.StandardOpenOption.CREATE_NEW
import static java.nio.file.StandardOpenOption.WRITE

class FileIoSpec extends RatpackGroovyDslSpec {

  def "can stream request body to file"() {
    given:
    def n = 10000
    def content = "a" * n
    def file = baseDir.path("foo")

    when:
    handlers {
      post {
        render FileIo.write(request.bodyStream, FileIo.open(file, CREATE_NEW, WRITE)).map { it.toString() }
      }
    }

    then:
    def r = request { it.post().body.text(content) }.body.text
    r == n.toString()

    and:
    file.text == content
  }

  def "can stream empty buffer to file"() {
    given:
    def file = baseDir.path("foo")

    when:
    handlers {
      get {
        render FileIo.write(Streams.empty(), FileIo.open(file, CREATE_NEW, WRITE)).map { it.toString() }
      }
    }

    then:
    text == "0"

    and:
    file.bytes.length == 0
  }

  def "propagates early stream error"() {
    given:
    def n = 10000
    def content = "a" * n
    def file = baseDir.path("foo")

    when:
    handlers {
      post {
        request.maxContentLength = n / 2
        FileIo.write(request.bodyStream, FileIo.open(file, CREATE_NEW, WRITE))
          .onError { render it.toString() }
          .then { render "bad" }
      }
    }

    then:
    def text = request { it.post().body.text(content) }.body.text
    text == "ratpack.http.RequestBodyTooLargeException: the request content length of 10000 exceeded the allowed maximum of 5000"

    and:
    // We opened the file, but didn't write anything.
    // The failure happened early because the content length was > allowed
    file.text == ""
  }

  def "propagates stream error"() {
    given:
    def file = baseDir.path("foo")

    when:
    handlers {
      get {
        def p = Streams.yield {
          it.requestNum < 2 ? Unpooled.wrappedBuffer("a".bytes) : {
            throw new Exception("!")
          }()
        }
        FileIo.write(p, FileIo.open(file, CREATE_NEW, WRITE))
          .onError { render it.toString() }
          .then { render "bad" }
      }
    }

    then:
    text == "java.lang.Exception: !"

    and:
    file.text == "aa"
  }

  def "can stream file"() {
    given:
    def n = 10000
    def content = "a" * n
    def file = baseDir.write("foo", content)

    when:
    handlers {
      get {
        response.sendStream(FileIo.readStream(FileIo.open(file, StandardOpenOption.READ), get(ByteBufAllocator), 8192))
      }
    }

    then:
    text == content
  }

  def "can partially stream file"() {
    given:
    def n = 10000
    def content = "a" * n
    def file = baseDir.write("foo", content)

    when:
    handlers {
      get {
        response.sendStream(FileIo.readStream(FileIo.open(file, StandardOpenOption.READ), get(ByteBufAllocator), 8192, 100, 300))
      }
    }

    then:
    text == "a" * 200
  }

  def "can write single buffer to file"() {
    when:
    def n = 10000
    def content = "a" * n
    def file = baseDir.path("foo")

    handlers {
      get {
        FileIo.write(Unpooled.wrappedBuffer(content.bytes), FileIo.open(file, CREATE_NEW, WRITE)).then {
          render "ok"
        }
      }
    }

    then:
    text == "ok"
    file.text == content
  }

  def "can write single buffer to position in file"() {
    when:
    def n = 10000
    def content = "a" * n
    def file = baseDir.path("foo")

    handlers {
      get {
        FileIo.write(Unpooled.wrappedBuffer(content.bytes), 10, FileIo.open(file, CREATE_NEW, WRITE)).then {
          render "ok"
        }
      }
    }

    then:
    text == "ok"
    file.bytes.length == content.bytes.length + 10
  }

  def "can read single buffer"() {
    given:
    def n = 10000
    def content = "a" * n
    def file = baseDir.write("foo", content)

    when:
    handlers {
      get {
        FileIo.read(FileIo.open(file, StandardOpenOption.READ), get(ByteBufAllocator), 8192).then(response.&send)
      }
    }

    then:
    text == content
  }

  def "can read single partial buffer"() {
    given:
    def n = 10000
    def content = "a" * n
    def file = baseDir.write("foo", content)

    when:
    handlers {
      get {
        FileIo.read(FileIo.open(file, StandardOpenOption.READ), get(ByteBufAllocator), 8192, 100, 200).then(response.&send)
      }
    }

    then:
    text == "a" * 100
  }
}
