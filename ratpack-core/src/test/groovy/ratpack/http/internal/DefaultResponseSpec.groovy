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

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import ratpack.exec.Blocking
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable

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
    def bufferedBody = Unpooled.wrappedBuffer(BODY.bytes)

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
    def bufferedBody = Unpooled.wrappedBuffer(BODY.bytes)

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
    def bufferedBody = Unpooled.wrappedBuffer(BODY.bytes)

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
    def bufferedBody = Unpooled.wrappedBuffer(BODY.bytes)

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

  def "can send files"() {
    given:
    def file = File.createTempFile("ratpackTest", "jpg")
    file << "abcd".getBytes("US-ASCII")
    def path = file.toPath()
    handlers {
      get {
        response.headers.set('content-length', 4)
        response.sendFile path
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
        response.beforeSend { it.status(202) }
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

  def "can finalize response before sending with async actions"() {
    given:
    handlers {
      get {
        response.beforeSend {
          Blocking.get { 1 }.then { response.headers.set("foo", 1) }
        }
        response.beforeSend {
          Blocking.get {
            2
          } then {
            response.headers.set("foo", response.headers.get("foo") + ":" + it)
          }
        }
        response.beforeSend {
          response.headers.set("foo", response.headers.get("foo") + ":3")
        }
        response.send()
      }
    }

    when:
    get()

    then:
    with(response) {
      headers.get("foo") == "1:2:3"
    }
  }

  def "can set connection close response header"() {
    given:
    def i = 0
    def channel = new BlockingVariable<Channel>(5)
    def closed = new BlockingVariable(5)
    handlers {
      get {
        if (i++ > 0) {
          response.forceCloseConnection()
          directChannelAccess.channel.closeFuture().addListener {
            closed.set(true)
          }
        } else {
          channel.set(directChannelAccess.channel)
        }

        response.send()
      }
    }

    when:
    def connection1 = applicationUnderTest.getAddress().toURL().openConnection()

    then:
    connection1.getHeaderField("Connection") == null
    channel.get().open

    when:
    def connection2 = applicationUnderTest.getAddress().toURL().openConnection()

    then:
    connection2.getHeaderField("Connection") == "close"
    closed.get()
    !channel.get().open
  }

  def "can add response finalizer"() {
    when:
    handlers {
      all {
        response.beforeSend {
          it.headers.add("foo", "1")
          response.beforeSend {
            it.headers.add("bar", "1")
          }
        }
        render "abc"
      }
    }

    then:
    def r = get()
    r.status.code == 200
    r.body.text == "abc"
    r.headers.foo == "1"
    r.headers.bar == "1"
  }
}

