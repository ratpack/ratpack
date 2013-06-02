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

package org.ratpackframework.path

import org.ratpackframework.test.DefaultRatpackSpec

import static org.ratpackframework.groovy.handling.ClosureHandlers.*

class PathAndMethodRoutingSpec extends DefaultRatpackSpec {

  def "can use path and method routes"() {
    when:
    app {
      handlers {
        add get("a/b/c") {
          response.send request.query
        }
        add path(":a/:b") {
          add handler(":c/:d") {
            methods.post {
              response.send allPathTokens.toString()
            }.named("put") {
              response.send allPathTokens.collectEntries { [it.key.toUpperCase(), it.value.toUpperCase()] }.toString()
            }.send()
          }
        }
      }
    }

    then:
    getText("a/b/c?foo=baz") == "foo=baz"
    resetRequest()
    postText("1/2/3/4") == "[a:1, b:2, c:3, d:4]"
    putText("5/6/7/8") == "[A:5, B:6, C:7, D:8]"
  }

  def "can use method chain"() {
    when:
    app {
      handlers {
        add handler("foo") {
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
}
