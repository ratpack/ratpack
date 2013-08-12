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

package org.ratpackframework.file

import com.jayway.restassured.response.Response
import io.netty.handler.codec.http.HttpHeaderDateFormat
import org.apache.commons.lang3.RandomStringUtils
import org.ratpackframework.test.DefaultRatpackSpec
import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpHeaders.Names.*
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED
import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static org.ratpackframework.groovy.handling.ClosureHandlers.fileSystem
import static org.ratpackframework.groovy.handling.ClosureHandlers.handler
import static org.ratpackframework.groovy.handling.ClosureHandlers.path
import static org.ratpackframework.handling.Handlers.assets
import static org.ratpackframework.handling.Handlers.post

class StaticFileSpec extends DefaultRatpackSpec {

  def "root listing disabled"() {
    given:
    file("public/static.text") << "hello!"
    file("public/foo/static.text") << "hello!"

    when:
    app {
      handlers {
        add assets("public")
      }
    }

    then:
    get("").statusCode == 403
    get("foo").statusCode == 403
    get("foos").statusCode == 404
  }

  def "can serve static file"() {
    given:
    def file = file("public/static.text") << "hello!"

    when:
    app {
      handlers {
        add assets("public")
      }
    }

    then:
    getText("static.text") == "hello!"
    getText("static.text") == "hello!"

    with(head("static.text")) {
      statusCode == 200
      asByteArray().length == 0
      getHeader("content-length") == file.length().toString()
    }
  }

  def "can serve index files"() {
    file("public/index.html") << "foo"
    file("public/dir/index.xhtml") << "bar"

    when:
    app {
      handlers {
        add assets("public", "index.html", "index.xhtml")
      }
    }

    then:
    getText() == "foo"
    getText("dir") == "bar"
    getText("dir/") == "bar"
  }

  def "can serve files with query strings"() {
    given:
    file("public/index.html") << "foo"

    when:
    app {
      handlers {
        add assets("public", "index.html")
      }
    }

    then:
    getText("?abc") == "foo"
    getText("index.html?abc") == "foo"
  }

  def "can map to multiple dirs"() {
    given:
    file("d1/f1.txt") << "1"
    file("d2/f2.txt") << "2"

    when:
    app {
      handlers {
        add path("a") {
          add assets("d1")
        }
        add path("b") {
          add assets("d2")
        }
      }
    }

    then:
    getText("a/f1.txt") == "1"
    getText("b/f2.txt") == "2"
  }

  def "decodes URL paths correctly"() {
    given:
    file("d1/some other.txt") << "1"
    file("d1/some+more.txt") << "2"
    file("d1/path to/some+where/test.txt") << "3"

    when:
    app {
      handlers {
        add assets("d1")
      }
    }

    then:
    getText("some%20other.txt") == "1"
    getText("some+more.txt") == "2"
    getText("path%20to/some+where/test.txt") == "3"
    get("some+other.txt").statusCode == 404
    get("some%20more.txt").statusCode == 404
  }

  def "can specify explicit not found handler"() {
    given:
    file("d1/f1.txt") << "1"
    file("d2/f2.txt") << "2"

    when:
    app {
      handlers {
        add assets("d1", handler {
          response.send("in not found handler")
        })
        add assets("d2")
      }
    }

    then:
    getText("f1.txt") == "1"
    getText("f2.txt") == "in not found handler"
  }

  def "can nest file system binding handlers"() {
    given:
    file("d1/d2/d3/dir/index.html") << "3"

    when:
    app {
      handlers {
        add fileSystem("d1") {
          add fileSystem("d2") {
            add assets("d3", "index.html")
          }
        }
      }
    }

    then:
    getText("dir") == "3"
  }

  def "can bind to subpath"() {
    given:
    file("foo/file.txt") << "file"

    when:
    app {
      handlers {
        add path("bar") {
          add assets("foo")
        }
      }
    }

    then:
    get("foo/file.txt").statusCode == 404
    getText("bar/file.txt") == "file"
  }

  def "files report a last modified time"() {
    given:
    def file = file("public/file.txt") << "hello!"

    and:
    app {
      handlers {
        add assets("public")
      }
    }

    expect:
    def response = get("file.txt")
    response.statusCode == 200
    // compare the last modified dates formatted as milliseconds are stripped when added as a response header
    formatDateHeader(parseDateHeader(response, LAST_MODIFIED)) == formatDateHeader(file.lastModified())
  }

  def "404 when posting to a non existent asset"() {
    given:
    file("public/file.txt") << "a"

    when:
    app {
      handlers {
        add assets("public")
      }
    }

    then:
    this.get("file.txt").statusCode == 200
    this.post("file.txt").statusCode == 405
    this.get("nothing").statusCode == 404
    this.post("nothing").statusCode == 404
  }

  @Unroll
  def "asset handler returns a #statusCode if file is #state the request's if-modified-since header"() {
    given:
    def file = file("public/file.txt") << "hello!"

    and:
    app {
      handlers {
        add assets("public")
      }
    }

    and:
    request.header IF_MODIFIED_SINCE, formatDateHeader(file.lastModified() + ifModifiedSince)

    expect:
    def response = get("file.txt")
    response.statusCode == statusCode.code()
    response.getHeader(CONTENT_LENGTH).toInteger() == contentLength

    where:
    ifModifiedSince | statusCode   | contentLength
    -1000           | OK           | "hello!".length()
    +1000           | OK           | "hello!".length()
    0               | NOT_MODIFIED | 0

    state = ifModifiedSince < 0 ? "newer than" : ifModifiedSince == 0 ? "the same age as" : "older than"
  }

  def "asset handler respect if-modified-since header when serving index files"() {
    given:
    def file = file("public/index.txt") << "hello!"

    and:
    app {
      handlers {
        add assets("public", "index.txt")
      }
    }

    and:
    request.header IF_MODIFIED_SINCE, formatDateHeader(file.lastModified())

    expect:
    def response = get("")
    response.statusCode == NOT_MODIFIED.code()
    response.getHeader(CONTENT_LENGTH).toInteger() == 0
  }

  def "can serve large static file"() {
    given:
    def file = file("public/static.text") << RandomStringUtils.randomAscii(fileSize)

    when:
    app {
      handlers {
        add assets("public")
      }
    }

    then:
    getText("static.text").bytes.length == file.length()

    with(head("static.text")) {
      statusCode == 200
      asByteArray().length == 0
      getHeader("content-length") == file.length().toString()
    }

    where:
    fileSize = 2131795 // taken from original bug report
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
