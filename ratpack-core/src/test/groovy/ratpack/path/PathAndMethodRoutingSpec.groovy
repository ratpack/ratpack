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

package ratpack.path

import ratpack.test.internal.RatpackGroovyDslSpec

class PathAndMethodRoutingSpec extends RatpackGroovyDslSpec {

  def "can use path and method routes"() {
    when:
    handlers {
      get("a/b/c") {
        response.headers.set("X-value", request.query)
        response.send request.query
      }
      prefix(":a/:b") {
        path(":c/:d") {
          byMethod {
            post {
              response.send new LinkedHashMap(allPathTokens).toString()
            }
            put {
              response.send allPathTokens.collectEntries { [it.key.toUpperCase(), it.value.toUpperCase()] }.toString()
            }
          }
        }
      }
    }

    then:
    getText("a/b/c?foo=baz") == "foo=baz"
    resetRequest()
    postText("1/2/3/4") == "[a:1, b:2, c:3, d:4]"
    putText("5/6/7/8") == "[A:5, B:6, C:7, D:8]"
    with(head("a/b/c?head")) {
      statusCode == 200
      headers.get("X-value") == "head"
      text.size() == 0
    }
  }

  def "can use method chain"() {
    when:
    handlers {
      path("foo") {
        def prefix = "common"
        byMethod {
          get {
            response.send("$prefix: get")
          }
          post {
            response.send("$prefix: post")
          }
        }
      }
    }

    then:
    getText("foo") == "common: get"
    postText("foo") == "common: post"
    put("foo").statusCode == 405
  }

  def "options requests are handled for single method handlers"() {
    when:
    handlers {
      get("foo") {
        render "foo"
      }
      post("bar") {
        render "bar"
      }
    }

    then:
    options("foo")
    response.headers.get("Allow") == "GET"
    response.statusCode == 200
    options("bar")
    response.headers.get("Allow") == "POST"
    response.statusCode == 200
  }

  def "options requests are handled for multi method handlers"() {
    when:
    handlers {
      all {
        byMethod {
          get {
            render "foo"
          }
          post {
            render "bar"
          }
        }
      }
    }

    then:
    options()
    response.headers.get("Allow") == "GET,POST"
  }

  def "options requests can be overridden for multi method handlers"() {
    when:
    handlers {
      all {
        byMethod {
          get {
            render "foo"
          }
          post {
            render "bar"
          }
          options {
            render "baz"
          }
        }
      }
    }

    then:
    options()
    response.body.getText() == "baz"
  }
}
