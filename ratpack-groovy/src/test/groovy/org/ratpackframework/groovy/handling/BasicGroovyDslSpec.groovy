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

package org.ratpackframework.groovy.handling

import org.ratpackframework.error.ServerErrorHandler
import org.ratpackframework.error.internal.DefaultServerErrorHandler
import org.ratpackframework.file.FileSystemBinding
import org.ratpackframework.file.internal.DefaultFileSystemBinding
import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

class BasicGroovyDslSpec extends RatpackGroovyDslSpec {

  def "can use special Groovy dsl"() {
    given:
    file("public/foo.txt") << "bar"

    when:
    app {
      handlers {
        get("a") {
          response.send "a handler"
        }
        post("b") {
          response.send "b handler"
        }
        prefix(":first") {
          get(":second") {
            response.send new LinkedHashMap<>(allPathTokens).toString()
          }
          path("c/:second") {
            response.send new LinkedHashMap<>(allPathTokens).toString()
          }
        }
        assets("public")
      }
    }

    then:
    getText("a") == "a handler"
    get("b").statusCode == 405
    postText("b") == "b handler"
    getText("1/2") == "[first:1, second:2]"
    getText("foo/c/bar") == "[first:foo, second:bar]"
    getText("foo.txt") == "bar"
  }

  def "can use file method to access file contextually"() {
    given:
    file("foo/file.txt") << "foo"
    file("bar/file.txt") << "bar"

    when:
    app {
      handlers {
        fileSystem("foo") {
          get("foo") {
            response.send file("file.txt").text
          }
        }
        fileSystem("bar") {
          get("bar") {
            response.send file("file.txt").text
          }
        }
      }
    }

    then:
    getText("foo") == 'foo'
    getText("bar") == 'bar'
  }

  def "can use method chain"() {
    when:
    app {
      handlers {
        path("foo") {
          def prefix = "common"

          methods.get {
            response.send("$prefix: get")
          }.post {
            response.send("$prefix: post")
          }.send()
        }
      }
    }

    then:
    getText("foo") == "common: get"
    postText("foo") == "common: post"
    put("foo").statusCode == 405
  }

  def "can inject services via closure params"() {
    when:
    app {
      handlers {
        get("p1") { FileSystemBinding fileSystemBinding ->
          response.send fileSystemBinding.class.name
        }
        get("p2") { FileSystemBinding fileSystemBinding, ServerErrorHandler serverErrorHandler ->
          response.send serverErrorHandler.class.name
        }
      }
    }

    then:
    getText("p1") == DefaultFileSystemBinding.class.name
    getText("p2") == DefaultServerErrorHandler.class.name
  }
}
