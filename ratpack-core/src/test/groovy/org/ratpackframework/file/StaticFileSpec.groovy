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

import org.ratpackframework.test.DefaultRatpackSpec

import static org.ratpackframework.routing.Handlers.assets
import static org.ratpackframework.routing.Handlers.assetsPath
import static org.ratpackframework.groovy.ClosureHandlers.fsContext
import static org.ratpackframework.groovy.ClosureHandlers.handler

class StaticFileSpec extends DefaultRatpackSpec {

  def "root listing disabled"() {
    given:
    file("public/static.text") << "hello!"
    file("public/foo/static.text") << "hello!"

    when:
    app {
      routing {
        route assets("public")
      }
    }

    then:
    urlGetConnection("").responseCode == 403
    urlGetConnection("foo").responseCode == 403
    urlGetConnection("foos").responseCode == 404
  }

  def "can serve static file"() {
    given:
    file("public/static.text") << "hello!"

    when:
    app {
      routing {
        route assets("public")
      }
    }

    then:
    urlGetText("static.text") == "hello!"
  }


  def "can serve index files"() {
    file("public/index.html") << "foo"
    file("public/dir/index.xhtml") << "bar"

    when:
    app {
      routing {
        route assets("public", "index.html", "index.xhtml")
      }
    }

    then:
    urlGetText() == "foo"
    urlGetText("dir") == "bar"
    urlGetText("dir/") == "bar"
  }

  def "can serve files with query strings"() {
    given:
    file("public/index.html") << "foo"

    when:
    app {
      routing {
        route assets("public", "index.html")
      }
    }

    then:
    urlGetText("?abc") == "foo"
    urlGetText("index.html?abc") == "foo"
  }

  def "can map to multiple dirs"() {
    given:
    file("d1/f1.txt") << "1"
    file("d2/f2.txt") << "2"

    when:
    app {
      routing {
        route assets("d1")
        if (exchange.request.queryParams.includeSecond) {
          route assets("d2")
        }
      }
    }

    then:
    urlGetText("f1.txt") == "1"
    urlGetConnection("f2.txt").responseCode == 404
    urlGetText("f2.txt?includeSecond") == "2"
  }

  def "can specify explicit not found handler"() {
    given:
    file("d1/f1.txt") << "1"
    file("d2/f2.txt") << "2"

    when:
    app {
      routing {
        route assets("d1", handler {
          response.send("in not found handler")
        })
        route assets("d2")
      }
    }

    then:
    urlGetText("f1.txt") == "1"
    urlGetText("f2.txt") == "in not found handler"
  }

  def "can nest file system contexts handlers"() {
    given:
    file("d1/d2/d3/dir/index.html") << "3"

    when:
    app {
      routing {
        route fsContext("d1") {
          route fsContext("d2") {
            route assets("d3", "index.html")
          }
        }
      }
    }

    then:
    urlGetText("dir") == "3"
  }

  def "can bind to subpath"() {
    given:
    file("foo/file.txt") << "file"

    when:
    app {
      routing {
        route assetsPath("bar", "foo")
      }
    }

    then:
    urlGetConnection("foo/file.txt").responseCode == 404
    urlGetText("bar/file.txt") == "file"
  }
}
