/*
 * Copyright 2017 the original author or authors.
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

class OptionsHttpMethodSpec extends RatpackGroovyDslSpec {

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
