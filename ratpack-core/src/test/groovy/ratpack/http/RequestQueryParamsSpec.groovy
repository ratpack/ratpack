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

package ratpack.http

import ratpack.test.internal.RatpackGroovyDslSpec

class RequestQueryParamsSpec extends RatpackGroovyDslSpec {

  def "query params are decoded correctly"() {
    when:
    handlers {
      get {
        response.send request.queryParams.toString()
      }
    }

    then:
    getText() == "[:]"
    resetRequest()

    then:
    params { it.put("a", "b") }
    getText("/") == "[a:[b]]"
    resetRequest()

    then:
    params {
      it.putAll("a", ["b", "c"]).put("d", "e")
    }
    getText("/") == "[a:[b, c], d:[e]]"
    resetRequest()

    then:
    params { it.put("abc", "") }
    getText("/") == "[abc:[]]"
    resetRequest()

    then:
    params { it.put("q", "https://tc.test?a=1&b=2") }
    getText("/") == "[q:[https://tc.test?a=1&b=2]]"
    resetRequest()

  }

}
