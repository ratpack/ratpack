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

package org.ratpackframework.http.internal

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec
import org.ratpackframework.util.internal.IoUtils

import static io.netty.handler.codec.http.HttpHeaders.Names.*
import static io.netty.handler.codec.http.HttpResponseStatus.OK

class DefaultResponseSpec extends RatpackGroovyDslSpec {

  private static final String BODY = "Hello!"

  def "can send byte array"() {
    given:
    app {
      handlers {
        get() {
          response.send "text/plain", BODY.bytes
        }
      }
    }

    when:
    get()

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("text/plain;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can set content type and override with send byte array"() {
    given:
    app {
      handlers {
        get() {
          response.setHeader(CONTENT_TYPE, "application/octet-stream")
          response.send "text/plain", BODY.bytes
        }
      }
    }

    when:
    get()

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("text/plain;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can send byte array with default content type"() {
    given:
    app {
      handlers {
        get() {
          response.send BODY.bytes
        }
      }
    }

    when:
    get()

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("application/octet-stream;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can set content type and not override with send byte array"() {
    given:
    app {
      handlers {
        get() {
          response.setHeader(CONTENT_TYPE, "application/octet-stream")
          response.send BODY.bytes
        }
      }
    }

    when:
    get()

    then:
    with (response) {
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
    app {
      handlers {
        get() {
          response.send "text/plain", bufferedBody
        }
      }
    }

    when:
    get()

    then:
    with (response) {
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
    app {
      handlers {
        get() {
          response.setHeader(CONTENT_TYPE, "application/octet-stream")
          response.send "text/plain", bufferedBody
        }
      }
    }

    when:
    get()

    then:
    with (response) {
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
    app {
      handlers {
        get() {
          response.send bufferedBody
        }
      }
    }

    when:
    get()

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("application/octet-stream;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

  def "can set content type and not override with send bytes"() {
    given:
    def bufferedBody = IoUtils.byteBuf(BODY.bytes)

    and:
    app {
      handlers {
        get() {
          response.setHeader(CONTENT_TYPE, "application/octet-stream")
          response.send bufferedBody
        }
      }
    }

    when:
    get()

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().equals(BODY)
      contentType.equals("application/octet-stream")
      header(CONTENT_LENGTH).toInteger() == BODY.length()
    }
  }

}