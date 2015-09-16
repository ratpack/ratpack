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

import static ratpack.http.MediaType.APPLICATION_FORM
import static ratpack.http.MediaType.APPLICATION_JSON

class ContentTypeHandlerSpec extends RatpackGroovyDslSpec {

  def "ok for valid"() {
    when:
    handlers {
      all(Handlers.contentTypes(APPLICATION_JSON, APPLICATION_FORM))
      all {
        render "ok"
      }
    }

    and:
    requestSpec {
      it.headers.add("Content-Type", "text/plain")
      it.body.stream({ it << "foo" })
    }

    then:
    post().statusCode == 415

    when:
    requestSpec {
      it.headers.add("Content-Type", APPLICATION_JSON)
      it.body.stream({ it << "[]" })
    }

    then:
    postText() == "ok"
    response.statusCode == 200

    when:
    resetRequest()
    requestSpec {
      it.headers.add("Content-Type", APPLICATION_FORM)
      it.body.stream({ it << [foo: "bar"].collect({ it }).join('&') })
    }

    then:
    postText() == "ok"
    response.statusCode == 200
  }

}
