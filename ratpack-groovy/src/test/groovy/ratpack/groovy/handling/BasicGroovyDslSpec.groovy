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

package ratpack.groovy.handling

import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultServerErrorHandler
import ratpack.file.FileSystemBinding
import ratpack.file.internal.DefaultFileSystemBinding
import ratpack.test.internal.RatpackGroovyDslSpec
import static ratpack.groovy.internal.ClosureUtil.with

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
        put("c") {
          response.send "c handler"
        }
        delete("d") {
          response.send "d handler"
        }
        prefix(":first") {
          get(":second") {
            response.send new LinkedHashMap<>(allPathTokens).toString()
          }
          handler("c/:second") {
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
    putText("c") == "c handler"
    deleteText("d") == "d handler"
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
        handler("foo") {
          def prefix = "common"
          byMethod {
            get {
              render "$prefix: get"
            }
            post {
              render "$prefix: post"
            }
          }

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
