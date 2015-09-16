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
import ratpack.error.internal.DefaultProductionErrorHandler
import ratpack.file.FileSystemBinding
import ratpack.file.internal.DefaultFileSystemBinding
import ratpack.test.internal.RatpackGroovyDslSpec

class BasicGroovyDslSpec extends RatpackGroovyDslSpec {

  def "can use special Groovy dsl"() {
    given:
    write "public/foo.txt", "bar"

    when:
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
        path("c/:second") {
          response.send new LinkedHashMap<>(allPathTokens).toString()
        }
      }
      files { dir "public" }
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
    write "foo/file.txt", "foo"
    write "bar/file.txt", "bar"

    when:
    handlers {
      fileSystem("foo") {
        get("foo") {
          render file("file.txt")
        }
      }
      fileSystem("bar") {
        get("bar") {
          render file("file.txt")
        }
      }
    }

    then:
    getText("foo") == 'foo'
    getText("bar") == 'bar'
  }

  def "can use method chain"() {
    when:
    handlers {
      path("foo") {
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

    then:
    getText("foo") == "common: get"
    postText("foo") == "common: post"
    put("foo").statusCode == 405
  }

  def "can inject services via closure params"() {
    when:
    handlers {
      get("p1") { FileSystemBinding fileSystemBinding ->
        render fileSystemBinding.class.name
      }
      get("p2") { FileSystemBinding fileSystemBinding, ServerErrorHandler serverErrorHandler ->
        render serverErrorHandler.class.name
      }
    }

    then:
    getText("p1") == DefaultFileSystemBinding.name
    getText("p2") == DefaultProductionErrorHandler.name
  }

  def "can nest"() {
    when:
    handlers {
      register {
        add(String, "bar")
      }
      insert {
        register {
          add(String, "foo")
        }
        get("foo") {
          render get(String)
        }
      }
      get("bar") {
        render get(String)
      }
    }

    then:
    getText("foo") == "foo"
    getText("bar") == "bar"
  }


}
