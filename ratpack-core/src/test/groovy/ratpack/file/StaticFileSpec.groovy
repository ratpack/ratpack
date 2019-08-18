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

import com.google.common.net.UrlEscapers
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import org.apache.commons.lang3.RandomStringUtils
import ratpack.func.Action
import ratpack.handling.HandlerDecorator
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.http.internal.HttpHeaderDateFormat
import ratpack.server.Stopper
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable
import spock.util.concurrent.PollingConditions

import static io.netty.handler.codec.http.HttpHeaders.Names.*
import static io.netty.handler.codec.http.HttpResponseStatus.*
import static java.nio.file.Files.getLastModifiedTime
import static java.nio.file.Files.size

class StaticFileSpec extends RatpackGroovyDslSpec {

  boolean compressResponses

  def setup() {
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        Multibinder.newSetBinder(binder(), HandlerDecorator).addBinding().toInstance(HandlerDecorator.prepend({
          it.response.beforeSend {
            if (!compressResponses) {
              it.noCompress()
            }
          }
          it.next()
        }))
      }
    }
  }

  def "requesting directory with no index file "() {
    given:
    write "public/static.text", "hello!"
    write "public/foo/static.text", "hello!"

    when:
    handlers {
      files { dir "public" }
      all {
        render "after"
      }
    }

    then:
    text == "after"
    getText("foo") == "after"
  }

  def "can serve static file"() {
    given:
    def file = write "public/static.text", "hello!"

    when:
    handlers {
      files { dir 'public' }
    }

    then:
    getText("static.text") == "hello!"
    getText("static.text") == "hello!"

    with(head("static.text")) {
      statusCode == OK.code()
      body.text.bytes.length == 0
      headers.get(CONTENT_LENGTH) == size(file).toString()
    }
  }

  def "can serve index files"() {
    write "public/index.html", "foo"
    write "public/dir/index.xhtml", "bar"

    given:
    requestSpec { r -> r.redirects 1 }

    when:
    handlers {
      files { dir "public" indexFiles "index.html", "index.xhtml" }
    }

    then:
    getText() == "foo"
    getText("dir") == "bar"
    getText("dir/") == "bar"
  }

  def "static files call onClose"() {
    given:
    write "public/index.html", "foo"
    def onCloseCalled = false
    def onCloseCalledWrapper = new BlockingVariable<Boolean>(1)

    when:
    handlers {
      get {
        onClose { onCloseCalled = true; onCloseCalledWrapper.set(onCloseCalled) }
        render file("public/index.html")
      }
    }

    then:
    getText() == "foo"
    onCloseCalledWrapper.get()

  }

  def "ensure that onClose is called after file is rendered"() {
    given:
    write "public/index.html", "foo"

    when:
    handlers {
      all { Stopper stopper ->
        onClose { stopper.stop() }
        next()
      }
      files { it.dir "public" }
      get {
        render file("public/index.html")
      }
    }

    then:
    getText(location) == "foo"
    new PollingConditions().within(10) {
      !server.running
    }

    where:
    location << ['', 'index.html']

  }

  def "index files are always served from a path with a trailing slash"() {
    given:
    write "public/dir/index.html", "bar"

    when:
    handlers {
      files { dir "public" indexFiles "index.html" }
    }

    then:
    //TODO the issue is we need to allow users to build their own urls if they want instead of doing it via the get()
    requestSpec { it.redirects 0 }

    if (key) {
      params { it.put(key, value) }
    }

    def suffix = key ? "?${UrlEscapers.urlFormParameterEscaper().escape(key)}=${UrlEscapers.urlFormParameterEscaper().escape(value)}" : ""

    with(get("dir")) {
      statusCode == 302
      headers.get(LOCATION) == "http://${server.bindHost}:${server.bindPort}/dir/${suffix}"
    }

    where:
    key     | value
    null    | null
    "a"     | "b"
    "a+b"   | "1+2"
    "a%20b" | "1%202"

  }

  def "can serve files with query strings"() {
    given:
    write "public/index.html", "foo"

    when:
    handlers {
      files { dir "public" indexFiles "index.html" }
    }

    then:
    getText("/?abc") == "foo"
    getText("index.html?abc") == "foo"
  }

  def "can map to multiple dirs"() {
    given:
    write "d1/f1.txt", "1"
    write "d2/f2.txt", "2"

    when:
    handlers {
      prefix("a") {
        files { dir "d1" }
      }
      prefix("b") {
        files { dir "d2" }
      }
    }

    then:
    getText("a/f1.txt") == "1"
    getText("b/f2.txt") == "2"
  }

  def "decodes URL paths correctly"() {
    given:
    write "d1/some other.txt", "1"
    write "d1/some+more.txt", "2"
    write "d1/path to/some+where/test.txt", "3"

    when:
    handlers {
      files { dir "d1" }
    }

    then:
//    getText("some%20other.txt") == "1"
    getText("some+more.txt") == "2"
//    getText("path%20to/some+where/test.txt") == "3"
//    get("some+other.txt").statusCode == NOT_FOUND.code()
//    get("some%20more.txt").statusCode == NOT_FOUND.code()
  }

  def "can nest file system binding handlers"() {
    given:
    write "d1/d2/d3/dir/index.html", "3"

    when:
    handlers {
      fileSystem("d1") {
        fileSystem("d2") {
          files { dir "d3" indexFiles "index.html" }
        }
      }
    }

    then:
    getText("dir/") == "3"
  }

  def "can bind to subpath"() {
    given:
    write("foo/file.txt", "file")

    when:
    handlers {
      prefix("bar") {
        files { dir "foo" }
      }
    }

    then:
    get("foo/file.txt").statusCode == NOT_FOUND.code()
    getText("bar/file.txt") == "file"
  }

  def "files report a last modified time"() {
    given:
    def file = write("public/file.txt", "hello!")

    and:
    handlers {
      files { dir "public" }
    }

    expect:
    def response = get("file.txt")
    response.statusCode == OK.code()
    // compare the last modified dates formatted as milliseconds are stripped when added as a response header
    formatDateHeader(parseDateHeader(response, LAST_MODIFIED)) == formatDateHeader(getLastModifiedTime(file).toMillis())
  }

  def "404 when posting to a non existent asset"() {
    given:
    write "public/file.txt", "a"

    when:
    handlers {
      files { dir "public" }
    }

    then:
    this.get("file.txt").statusCode == OK.code()
    this.post("file.txt").statusCode == METHOD_NOT_ALLOWED.code()
    this.get("nothing").statusCode == NOT_FOUND.code()
    this.post("nothing").statusCode == NOT_FOUND.code()
  }

  def "asset handler returns a #statusCode if file is #state the request's if-modified-since header"() {
    given:
    def file = write("public/file.txt", "hello!")

    and:
    handlers {
      files { dir "public" }
    }

    and:
    def headerValue = formatDateHeader(getLastModifiedTime(file).toMillis() + ifModifiedSince)
    requestSpec { RequestSpec request ->
      request.headers.add(IF_MODIFIED_SINCE, headerValue)
    } as Action<? super RequestSpec>

    expect:
    def response = get("file.txt")
    response.statusCode == statusCode.code()
    if (!compressResponses) {
      if (contentLength == null) {
        assert !response.headers.contains(CONTENT_LENGTH)
      } else {
        assert response.headers.get(CONTENT_LENGTH).toInteger() == contentLength
      }
    }

    where:
    ifModifiedSince | statusCode   | contentLength
    -1000           | OK           | "hello!".length()
    +1000           | NOT_MODIFIED | null
    0               | NOT_MODIFIED | null

    state = ifModifiedSince < 0 ? "newer than" : ifModifiedSince == 0 ? "the same age as" : "older than"
  }

  def "asset handler respect if-modified-since header when serving index files"() {
    given:
    def file = write("public/index.txt", "hello!")

    and:
    handlers {
      files { dir "public" indexFiles "index.txt" }
    }

    and:
    def headerValue = formatDateHeader(getLastModifiedTime(file).toMillis())
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add(IF_MODIFIED_SINCE, headerValue) } as Action<? super RequestSpec>

    expect:
    def response = get("")
    response.statusCode == NOT_MODIFIED.code()
    if (!compressResponses) {
      assert !response.headers.contains(CONTENT_LENGTH)
    }

  }

  def "can serve large static file"() {
    given:
    def file = write("public/static.text", RandomStringUtils.randomAscii(fileSize))

    when:
    handlers {
      files { dir "public" }
    }

    then:
    getText("static.text").bytes.length == size(file)

    with(head("static.text")) {
      statusCode == OK.code()
      body.bytes.length == 0
      if (!compressResponses) {
        assert headers.get(CONTENT_LENGTH) == size(file).toString()
      }
    }

    where:
    fileSize = 2131795 // taken from original bug report
  }

  def "asset handler passes through on not found"() {
    given:
    write "public/foo.txt", "bar"

    when:
    handlers {
      files { dir "public" }
      all { render "after" }
    }

    then:
    getText("foo.txt") == "bar"
    response.statusCode == 200
    getText("other") == "after"
  }

  def "only serve files from within the binding root"() {
    setup:
    write("public/foo.txt", "bar")
    handlers {
      files { dir "public" }
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

  def "only supports GET"() {
    when:
    write("public/foo.txt", "bar")
    handlers {
      files { dir "public" }
    }

    then:
    getText("foo.txt") == "bar"
    post("foo.txt").statusCode == 405
  }

  def "can use path that looks like a URL"() {
    when:
    write("public/data:application/json", "bar")
    handlers {
      files { dir "public" }
    }

    then:
    getText("/data:application/json") == "bar"
  }

  def "decodes path to find file"() {
    when:
    write("public/foo]", "bar")
    handlers {
      files { dir "public" }
    }

    then:
    getText("/foo%5D") == "bar"
  }

  private static Date parseDateHeader(ReceivedResponse response, String name) {
    HttpHeaderDateFormat.get().parse(response.headers.get(name))
  }

  private static String formatDateHeader(Long timestamp) {
    formatDateHeader(new Date(timestamp))
  }

  private static String formatDateHeader(Date date) {
    HttpHeaderDateFormat.get().format(date)
  }
}
