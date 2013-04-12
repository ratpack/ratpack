/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework

import org.ratpackframework.test.DefaultRatpackSpec

class RoutingSpec extends DefaultRatpackSpec {

  def "can route all"() {
    given:
    routing {
      all("/a") {
        text it.method
      }

      all("/b") {
        text it.method
      }
    }

    when:
    startApp()

    then:
    urlGetText("a") == "GET"
    urlPostText("a") == "POST"
    urlGetText("b") == "GET"
    urlPostText("b") == "POST"
  }

  def "can route with regex"() {
    given:
    routing {
      getRe("/a(.+)") {
        text "a:" + request.pathParams
      }
      getRe("/b(.+)") {
        text "b:" + request.pathParams
      }
    }

    when:
    startApp()

    then:
    urlGetText("abc/de") == "a:[0:bc/de]"
    urlGetText("bbc/de") == "b:[0:bc/de]"
  }

  def "using wrong http method to access an endpoint produces a 405"() {
    given:
    routing {
      get("/foo") {
        text "foo"
      }
    }

    when:
    startApp()

    then:
    urlPostConnection("foo").responseCode == 405
  }

}
