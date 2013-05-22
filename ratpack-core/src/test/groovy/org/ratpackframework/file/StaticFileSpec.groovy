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

import static org.ratpackframework.groovy.ClosureHandlers.fileSystem
import static org.ratpackframework.groovy.ClosureHandlers.handler
import static org.ratpackframework.groovy.ClosureHandlers.path
import static org.ratpackframework.handling.Handlers.assets
import static org.ratpackframework.handling.Handlers.assetsPath

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
    file("public/static.text") << "hello!"

    when:
    app {
      handlers {
        add assets("public")
      }
    }

    then:
    getText("static.text") == "hello!"
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

  def "can nest file system contexts handlers"() {
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
        add assetsPath("bar", "foo")
      }
    }

    then:
    get("foo/file.txt").statusCode == 404
    getText("bar/file.txt") == "file"
  }
}
