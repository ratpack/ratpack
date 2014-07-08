/*
 * Copyright 2013 the original author or authors.
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

package ratpack.http.internal

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.http.HttpResponseChunk
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.util.internal.IoUtils

import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import static io.netty.handler.codec.http.HttpResponseStatus.OK

class DefaultResponseSpec extends RatpackGroovyDslSpec {

  private static final String BODY = "Hello!"

  def "can send byte array"() {
    given:
    handlers {
      get {
        response.send "text/plain", BODY.bytes
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("text/plain;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can set content type and override with send byte array"() {
    given:
    handlers {
      get {
        response.headers.set(CONTENT_TYPE, "application/octet-stream")
        response.send "text/plain", BODY.bytes
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("text/plain;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can send byte array with default content type"() {
    given:
    handlers {
      get {
        response.send BODY.bytes
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("application/octet-stream")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can set content type and not override with send byte array"() {
    given:
    handlers {
      get {
        response.headers.set(CONTENT_TYPE, "application/octet-stream")
        response.send BODY.bytes
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("application/octet-stream")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can send bytes"() {
    given:
    def bufferedBody = IoUtils.byteBuf(BODY.bytes)

    and:
    handlers {
      get {
        response.send "text/plain", bufferedBody
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("text/plain;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can set content type and override with send bytes"() {
    given:
    def bufferedBody = IoUtils.byteBuf(BODY.bytes)

    and:
    handlers {
      get {
        response.headers.set(CONTENT_TYPE, "application/octet-stream")
        response.send "text/plain", bufferedBody
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("text/plain;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can send bytes with default content type"() {
    given:
    def bufferedBody = IoUtils.byteBuf(BODY.bytes)

    and:
    handlers {
      get {
        response.send bufferedBody
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("application/octet-stream")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can set content type and not override with send bytes"() {
    given:
    def bufferedBody = IoUtils.byteBuf(BODY.bytes)

    and:
    handlers {
      get {
        response.headers.set(CONTENT_TYPE, "application/foo")
        response.send bufferedBody
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("application/foo")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can send empty response"() {
    given:
    handlers {
      get {
        response.send()
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == OK.code()
      body.asString().empty
      contentType.equals("")
      header(CONTENT_LENGTH).toInteger() == 0
    }
  }

  def "can send input streams"() {
    when:
    def string = "a" * 1024 * 10
    def bytes = string.getBytes("utf8")

    handlers {
      handler {
        response.send "text/plain;charset=UTF-8", new ByteArrayInputStream(bytes)
      }
    }

    then:
    text == string
  }

  def "can send files"() {
    given:
    def file = File.createTempFile("ratpackTest", "jpg")
    file << "abcd".bytes
    def path = file.toPath()
    handlers {
      get {
        response.sendFile context, Files.readAttributes(path, BasicFileAttributes), path
      }
    }

    when:
    get()

    then:
    text == "abcd"
  }

  /**
   * This is just a test class and does not strictly adhere to the reactive-streams spec.
   */
  static class LargeContentPublisher implements Publisher<String> {
    boolean started

    @Override
    void subscribe(Subscriber<String> subscriber) {
      Subscription subscription = new Subscription() {
        @Override
        void cancel() {}

        @Override
        void request(int elements) {
          if (!started) {
            started = true
            Thread.start {
              "This is a really long string that needs to be sent chunked".toList().collate(20).each {
                subscriber.onNext(new HttpResponseChunk(it.join('')))
                Thread.sleep(500)
              }

              subscriber.onComplete()
            }
          }
        }
      }

      subscriber.onSubscribe(subscription)
    }
  }

  def "can send chunked response"() {
    when:
    handlers {
      handler {
        response.send(context, new LargeContentPublisher())
      }
    }

    then:
    def response = get()
    response.statusCode == OK.code()
    response.header("Content-Length") == "0"
    response.header("Transfer-Encoding") == "chunked"
    response.body.asString() == "14\r\nThis is a really lon\r\n14\r\ng string that needs \r\n12\r\nto be sent chunked\r\n0\r\n\r\n"
  }
}