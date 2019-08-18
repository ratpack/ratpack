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

package ratpack.file.internal

import ratpack.func.Action
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.http.internal.HttpHeaderDateFormat
import ratpack.test.internal.RatpackGroovyDslSpec

import java.nio.file.Files
import java.nio.file.Path

import static io.netty.handler.codec.http.HttpHeaders.Names.*
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED
import static io.netty.handler.codec.http.HttpResponseStatus.OK

class FileRenderingSpec extends RatpackGroovyDslSpec {

  private static final String FILE_CONTENTS = "hello!"
  private Path myFile

  def setup() {
    myFile = write("myFile.text", FILE_CONTENTS)
  }

  def "can render file"() {
    given:
    handlers {
      get("path") { render myFile }
    }

    when:
    get("path")

    then:
    with(response) {
      statusCode == OK.code()
      body.text.contains(FILE_CONTENTS)
      headers.get(CONTENT_TYPE) == "text/plain"
      headers.get(CONTENT_LENGTH).toInteger() == FILE_CONTENTS.length()
    }
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
        render myFile
      }
    }

    then:
    def r = get()
    r.status.code == 200
    r.body.text.contains(FILE_CONTENTS)
    r.headers.foo == "1"
    r.headers.bar == "1"
  }

  def "renderer respect if-modified-since header"() {
    given:
    handlers {
      get("path") { render myFile }
    }

    and:
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add IF_MODIFIED_SINCE, formatDateHeader(Files.getLastModifiedTime(myFile).toMillis()) } as Action<? super RequestSpec>

    when:
    get("path")

    then:
    with(response) {
      statusCode == NOT_MODIFIED.code()
      !headers.contains(CONTENT_LENGTH)
    }
  }

  def "files report a last modified time"() {
    given:
    handlers {
      get("path") { render myFile }
    }

    when:
    get("path")

    then:
    response.statusCode == OK.code()
    // compare the last modified dates formatted as milliseconds are stripped when added as a response header
    formatDateHeader(parseDateHeader(response, LAST_MODIFIED)) == formatDateHeader(Files.getLastModifiedTime(myFile).toMillis())
  }

  def "can render in response to non GET"() {
    when:
    handlers {
      post { render myFile }
    }

    then:
    postText() == myFile.text
  }

  private static Date parseDateHeader(ReceivedResponse response, String name) {
    HttpHeaderDateFormat.get().parse(response.headers.get(name))
  }

  private static String formatDateHeader(long timestamp) {
    formatDateHeader(new Date(timestamp))
  }

  private static String formatDateHeader(Date date) {
    HttpHeaderDateFormat.get().format(date)
  }
}
