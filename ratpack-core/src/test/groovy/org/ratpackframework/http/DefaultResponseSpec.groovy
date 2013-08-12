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

package org.ratpackframework.http

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec
import static io.netty.handler.codec.http.HttpHeaders.Names.*
import static io.netty.handler.codec.http.HttpResponseStatus.OK

class DefaultResponseSpec extends RatpackGroovyDslSpec {

  private static final String BODY = "Hello!"
  private static final String HTML_BODY = "<HTML><BODY><H1>HELLO!</H1></BODY></HTML>"
  private static final String XML_BODY = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><MESSAGE>HELLO!</MESSAGE>"

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

  def "can send byte array guessing string content"() {
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

  def "can send byte array guessing HTML content"() {
    given:
    app {
      handlers {
        get() {
          response.send HTML_BODY.bytes
        }
      }
    }

    when:
    get()

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().equals(HTML_BODY)
      contentType.equals("text/html;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == HTML_BODY.length()
    }
  }

  def "can send byte array guessing XML content"() {
    given:
    app {
      handlers {
        get() {
          response.send XML_BODY.bytes
        }
      }
    }

    when:
    get()

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().equals(XML_BODY)
      contentType.equals("application/xml;charset=UTF-8")
      header(CONTENT_LENGTH).toInteger() == XML_BODY.length()
    }
  }

}