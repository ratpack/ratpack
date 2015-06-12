/*
 * Copyright 2014 the original author or authors.
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

package ratpack.http.internal

import ratpack.handling.Handlers
import ratpack.test.internal.RatpackGroovyDslSpec

import static ratpack.http.MediaType.APPLICATION_JSON

class AcceptsHandlerSpec extends RatpackGroovyDslSpec {

  def "ok for valid"() {
    when:
    handlers {
      all(Handlers.accepts(APPLICATION_JSON, "application/xml"))
      all {
        byContent {
          json {
            render "[]"
          }
          xml {
            render "<foo/>"
          }
        }
      }
    }

    and:
    requestSpec { it.headers.add("Accept", "foo/bar") }

    then:
    get().statusCode == 406

    when:
    requestSpec { it.headers.add("Accept", APPLICATION_JSON) }

    then:
    text == "[]"
    response.statusCode == 200

    when:
    resetRequest()
    requestSpec { it.headers.add("Accept", "application/xml") }

    then:
    text == "<foo/>"
    response.statusCode == 200
  }

}
