/*
 * Copyright 2015 the original author or authors.
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

package ratpack.http

import ratpack.test.internal.RatpackGroovyDslSpec

class ExpectContinueSpec extends RatpackGroovyDslSpec {

  def "can handle expect continue"() {
    when:
    handlers {
      get { render "ok" }
    }

    then:
    with(request { it.headers.add("Expect", "100-Continue") }) {
      statusCode == 100
      body.text == ""
    }
    text == "ok"
  }

  def "can handle expect continue when client erroneously sends body"() {
    when:
    handlers {
      post { render request.body.map { it.text } }
    }

    then:
    with(request { it.method("post").headers { it.add("Expect", "100-Continue") }.body.text("foo") }) {
      statusCode == 100
      body.text == ""
    }
    request { it.method("post").body.text("foo") }.body.text == "foo"
  }

}
