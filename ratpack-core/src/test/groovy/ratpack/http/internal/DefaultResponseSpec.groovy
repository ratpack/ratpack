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

import ratpack.http.ResponseMetaData
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.util.internal.IoUtils

import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED
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
      body.text.equals(BODY)
      headers.get(CONTENT_TYPE) == "text/plain"
      headers.get(CONTENT_LENGTH).toInteger() == BODY.length()
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
      body.text.equals(BODY)
      headers.get(CONTENT_TYPE) == "text/plain"
      headers.get(CONTENT_LENGTH).toInteger() == BODY.length()
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
      body.text.equals(BODY)
      headers.get(CONTENT_TYPE) == "application/octet-stream"
      headers.get(CONTENT_LENGTH).toInteger() == BODY.length()
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
      body.text.equals(BODY)
      headers.get(CONTENT_TYPE) == "application/octet-stream"
      headers.get(CONTENT_LENGTH).toInteger() == BODY.length()
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
      body.text.equals(BODY)
      headers.get(CONTENT_TYPE) == "text/plain"
      headers.get(CONTENT_LENGTH).toInteger() == BODY.length()
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
      body.text.equals(BODY)
      headers.get(CONTENT_TYPE) == "text/plain"
      headers.get(CONTENT_LENGTH).toInteger() == BODY.length()
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
      body.text.equals(BODY)
      headers.get(CONTENT_TYPE) == "application/octet-stream"
      headers.get(CONTENT_LENGTH).toInteger() == BODY.length()
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
      body.text.equals(BODY)
      headers.get(CONTENT_TYPE) == "application/foo"
      headers.get(CONTENT_LENGTH).toInteger() == BODY.length()
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
      body.text.empty
      !headers.get(CONTENT_TYPE)
      headers.get(CONTENT_LENGTH).toInteger() == 0
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
        response.sendFile Files.readAttributes(path, BasicFileAttributes), path
      }
    }

    when:
    get()

    then:
    text == "abcd"
  }

  def "can finalize response before sending"() {
    given:
    handlers {
      get {
        response.beforeSend { ResponseMetaData responseMetaData ->
          responseMetaData.status(ACCEPTED.code())
        }
        response.send()
      }
    }

    when:
    get()

    then:
    with(response) {
      statusCode == ACCEPTED.code()
      body.text.empty
      !headers.get(CONTENT_TYPE)
      headers.get(CONTENT_LENGTH).toInteger() == 0
    }

  }

}

