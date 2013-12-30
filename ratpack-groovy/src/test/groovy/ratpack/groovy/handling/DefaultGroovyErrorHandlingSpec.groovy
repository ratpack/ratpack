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

import ratpack.test.internal.RatpackGroovyAppSpec

class DefaultGroovyErrorHandlingSpec extends RatpackGroovyAppSpec {

  def "error handler is registered"() {
    given:
    handlers {
      get {
        throw new Exception("!")
      }
    }

    when:
    get()

    then:
    response.statusCode == 500
    response.body.asString().contains "html"
  }

  def "error handler is registered next() is not called"() {
    given:
    handlers {
      prefix("foo") {
        handler {
          throw new Exception("!")
        }
      }
    }

    when:
    get("foo")

    then:
    response.statusCode == 500
    response.body.asString().contains "html"
  }

  def "404 page is used"() {
    given:
    handlers {
      get("foo") {
        response.send("foo")
      }
    }

    when:
    get("bar")

    then:
    response.statusCode == 404
    response.body.asString().contains "html"
  }

}
