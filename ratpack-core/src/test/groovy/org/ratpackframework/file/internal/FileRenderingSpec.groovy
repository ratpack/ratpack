/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.file.internal

import com.jayway.restassured.response.Response
import io.netty.handler.codec.http.HttpHeaderDateFormat
import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

import static io.netty.handler.codec.http.HttpHeaders.Names.*
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED
import static io.netty.handler.codec.http.HttpResponseStatus.OK

class FileRenderingSpec extends RatpackGroovyDslSpec {

  private static final String FILE_CONTENTS = "hello!"
  private File myFile

  def setup() {
    myFile = file("myFile.text") << FILE_CONTENTS
  }

  def "can render file"() {
    given:
    app {
      handlers {
        get("path") {render myFile}
      }
    }

    when:
    get("path")

    then:
    with (response) {
      statusCode == OK.code()
      response.body.asString().contains(FILE_CONTENTS)
      response.contentType.equals("text/plain;charset=UTF-8")
      response.header(CONTENT_LENGTH).toInteger() == FILE_CONTENTS.length()
    }
  }

  def "renderer respect if-modified-since header"() {
    given:
    app {
      handlers {
        get("path") {render myFile}
      }
    }

    and:
    request.header IF_MODIFIED_SINCE, formatDateHeader(myFile.lastModified())

    when:
    get("path")

    then:
    with (response) {
      statusCode ==  NOT_MODIFIED.code()
      response.header(CONTENT_LENGTH).toInteger() == 0
    }
  }

  def "files report a last modified time"() {
    given:
    app {
      handlers {
        get("path") {render myFile}
      }
    }

    when:
    get("path")

    then:
    with (response) {
      statusCode == OK.code()
      // compare the last modified dates formatted as milliseconds are stripped when added as a response header
      formatDateHeader(parseDateHeader(response, LAST_MODIFIED)) == formatDateHeader(myFile.lastModified())
    }

  }

  private static Date parseDateHeader(Response response, String name) {
    new HttpHeaderDateFormat().parse(response.getHeader(name))
  }

  private static String formatDateHeader(long timestamp) {
    formatDateHeader(new Date(timestamp))
  }

  private static String formatDateHeader(Date date) {
    new HttpHeaderDateFormat().format(date)
  }
}
