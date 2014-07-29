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

package ratpack.file

import com.jayway.restassured.response.Response
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.conn.HttpHostConnectException
import ratpack.http.internal.HttpHeaderDateFormat
import ratpack.launch.LaunchException
import ratpack.server.Stopper
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import java.util.concurrent.RejectedExecutionException

import static io.netty.handler.codec.http.HttpHeaders.Names.*
import static io.netty.handler.codec.http.HttpResponseStatus.*
import static java.nio.file.Files.getLastModifiedTime
import static java.nio.file.Files.size

class StaticFileSpec extends RatpackGroovyDslSpec {

  def "requesting directory with no index file "() {
    given:
    file "public/static.text", "hello!"
    file "public/foo/static.text", "hello!"

    when:
    handlers {
      assets("public")
      handler {
        render "after"
      }
    }

    then:
    text == "after"
    getText("foo") == "after"
  }

  def "can serve static file"() {
    given:
    def file = file "public/static.text", "hello!"

    when:
    handlers {
      assets("public")
    }

    then:
    getText("static.text") == "hello!"
    getText("static.text") == "hello!"

    with(head("static.text")) {
      statusCode == OK.code()
      asByteArray().length == 0
      getHeader("content-length") == size(file).toString()
    }
  }

  def "can serve index files"() {
    file "public/index.html", "foo"
    file "public/dir/index.xhtml", "bar"

    when:
    handlers {
      assets("public", "index.html", "index.xhtml")
    }

    then:
    getText() == "foo"
    getText("dir") == "bar"
    getText("dir/") == "bar"
  }

  def "static files call onClose"() {
    given:
    file "public/index.html", "foo"
    def counter = 0

    when:
    handlers {
      get {
        onClose { counter++ }
        render file("public/index.html")
      }
    }

    then:
    getText() == "foo"
    new PollingConditions().within(3) {
      counter == 1
    }

  }

  @Unroll
  def "ensure that onClose is called after file is rendered"() {
    given:
    file "public/index.html", "foo"

    when:
    handlers {
      handler { Stopper stopper ->
        onClose { stopper.stop() }
        next()
      }
      assets("public")
      get {
        render file("public/index.html")
      }
    }

    then:
    getText(location) == "foo"
    !server.running

    where:
    location << ['', 'index.html']

  }

  @Unroll
  "index files are always served from a path with a trailing slash"() {
    given:
    file "public/dir/index.html", "bar"
    createRequest()
    request.redirects().follow(false)

    when:
    handlers {
      assets("public", "index.html")
    }

    then:
    with(get("dir$suffix")) {
      statusCode == 302
      getHeader(LOCATION) == "http://${server.bindHost}:${server.bindPort}/dir/$suffix"
    }

    where:
    suffix << ["", "?a=b", "?a+b=1+2", "?a%20b=1%202"]
  }

  def "can serve files with query strings"() {
    given:
    file "public/index.html", "foo"

    when:
    handlers {
      assets("public", "index.html")
    }

    then:
    getText("?abc") == "foo"
    getText("index.html?abc") == "foo"
  }

  def "can map to multiple dirs"() {
    given:
    file "d1/f1.txt", "1"
    file "d2/f2.txt", "2"

    when:
    handlers {
      prefix("a") {
        assets("d1")
      }
      prefix("b") {
        assets("d2")
      }
    }

    then:
    getText("a/f1.txt") == "1"
    getText("b/f2.txt") == "2"
  }

  def "decodes URL paths correctly"() {
    given:
    file "d1/some other.txt", "1"
    file "d1/some+more.txt", "2"
    file "d1/path to/some+where/test.txt", "3"

    when:
    handlers {
      assets("d1")
    }

    then:
    getText("some%20other.txt") == "1"
    getText("some+more.txt") == "2"
    getText("path%20to/some+where/test.txt") == "3"
    get("some+other.txt").statusCode == NOT_FOUND.code()
    get("some%20more.txt").statusCode == NOT_FOUND.code()
  }

  def "can nest file system binding handlers"() {
    given:
    file "d1/d2/d3/dir/index.html", "3"

    when:
    handlers {
      fileSystem("d1") {
        fileSystem("d2") {
          assets("d3", "index.html")
        }
      }
    }

    then:
    getText("dir") == "3"
  }

  def "can bind to subpath"() {
    given:
    file("foo/file.txt", "file")

    when:
    handlers {
      prefix("bar") {
        assets("foo")
      }
    }

    then:
    get("foo/file.txt").statusCode == NOT_FOUND.code()
    getText("bar/file.txt") == "file"
  }

  def "files report a last modified time"() {
    given:
    def file = file("public/file.txt", "hello!")

    and:
    handlers {
      assets("public")
    }

    expect:
    def response = get("file.txt")
    response.statusCode == OK.code()
    // compare the last modified dates formatted as milliseconds are stripped when added as a response header
    formatDateHeader(parseDateHeader(response, LAST_MODIFIED)) == formatDateHeader(getLastModifiedTime(file).toMillis())
  }

  def "404 when posting to a non existent asset"() {
    given:
    file "public/file.txt", "a"

    when:
    handlers {
      assets("public")
    }

    then:
    this.get("file.txt").statusCode == OK.code()
    this.post("file.txt").statusCode == METHOD_NOT_ALLOWED.code()
    this.get("nothing").statusCode == NOT_FOUND.code()
    this.post("nothing").statusCode == NOT_FOUND.code()
  }

  @Unroll
  def "asset handler returns a #statusCode if file is #state the request's if-modified-since header"() {
    given:
    def file = file("public/file.txt", "hello!")

    and:
    handlers {
      assets("public")
    }

    and:
    request.header IF_MODIFIED_SINCE, formatDateHeader(getLastModifiedTime(file).toMillis() + ifModifiedSince)

    expect:
    def response = get("file.txt")
    response.statusCode == statusCode.code()
    if (!application.server.launchConfig.compressResponses) {
      assert response.getHeader(CONTENT_LENGTH).toInteger() == contentLength
    }

    where:
    ifModifiedSince | statusCode   | contentLength
    -1000           | OK           | "hello!".length()
    +1000           | OK           | "hello!".length()
    0               | NOT_MODIFIED | 0

    state = ifModifiedSince < 0 ? "newer than" : ifModifiedSince == 0 ? "the same age as" : "older than"
  }

  def "asset handler respect if-modified-since header when serving index files"() {
    given:
    def file = file("public/index.txt", "hello!")

    and:
    handlers {
      assets("public", "index.txt")
    }

    and:
    request.header IF_MODIFIED_SINCE, formatDateHeader(getLastModifiedTime(file).toMillis())

    expect:
    def response = get("")
    response.statusCode == NOT_MODIFIED.code()
    if (!application.server.launchConfig.compressResponses) {
      assert response.getHeader(CONTENT_LENGTH).toInteger() == 0
    }

  }

  def "can serve large static file"() {
    given:
    def file = file("public/static.text", RandomStringUtils.randomAscii(fileSize))

    when:
    handlers {
      assets("public")
    }

    then:
    getText("static.text").bytes.length == size(file)

    with(head("static.text")) {
      statusCode == OK.code()
      asByteArray().length == 0
      if (!application.server.launchConfig.compressResponses) {
        assert getHeader("content-length") == size(file).toString()
      }
    }

    where:
    fileSize = 2131795 // taken from original bug report
  }

  def "can serve index files using global configuration"() {
    given:
    file "d1/custom.html", "foo"
    file "d2/custom.xhtml", "bar"

    and:
    launchConfig {
      indexFiles "custom.xhtml", "custom.html"
    }

    when:
    handlers {
      prefix("a") {
        assets("d1")
      }
      prefix("b") {
        assets("d2")
      }
    }

    then:
    getText("a") == "foo"
    getText("b") == "bar"
  }

  def "can serve index files overriding global configuration with handler"() {
    given:
    file "public/custom.html", "foo"
    file "public/index.html", "bar"

    and:
    launchConfig {
      indexFiles "custom.html"
    }

    when:
    handlers {
      assets("public", "index.html")
    }

    then:
    getText() == "bar"
  }

  def "can serve index files overriding global configuration with module"() {
    given:
    file "public/custom.html", "foo"
    file "public/index.html", "bar"
    and:
    launchConfig {
      indexFiles "index.html", "custom.html"
    }

    when:
    handlers {
      assets("public")
    }

    then:
    getText() == "bar"
  }

  def "asset handler passes through on not found"() {
    given:
    file "public/foo.txt", "bar"

    when:
    handlers {
      assets "public"
      handler { render "after" }
    }

    then:
    getText("foo.txt") == "bar"
    response.statusCode == 200
    getText("other") == "after"
  }

  def "only serve files from within the binding root"() {
    setup:
    file("public/foo.txt", "bar")
    handlers {
      assets "public"
    }

    when:
    getText("foo.txt")

    then:
    response.statusCode == 200

    when:
    getText("bar.txt")

    then:
    response.statusCode == 404

    when:
    getText("../../../etc/passwd")

    then:
    response.statusCode == 404
  }

  private static Date parseDateHeader(Response response, String name) {
    HttpHeaderDateFormat.get().parse(response.getHeader(name))
  }

  private static String formatDateHeader(long timestamp) {
    formatDateHeader(new Date(timestamp))
  }

  private static String formatDateHeader(Date date) {
    HttpHeaderDateFormat.get().format(date)
  }
}
