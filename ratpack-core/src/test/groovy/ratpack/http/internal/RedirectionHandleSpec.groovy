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

import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec

class RedirectionHandleSpec extends RatpackGroovyDslSpec {

  @Override
  void configureRequest(RequestSpec requestSpecification) {
    requestSpecification.redirects(0)
  }

  def "ok for valid"() {
    given:
    handlers {
      redirect(301, 'http://www.ratpack.io')
    }

    when:
    get()

    then:
    response.statusCode == 301
    response.headers['Location'] == 'http://www.ratpack.io'
  }

  def "it checks that the desired status code is a 3XX one"() {
    given:
    handlers {
      redirect(statusCode, 'http://www.ratpack.io')
    }

    when:
    get()

    then:
    thrown IllegalArgumentException

    where:
    statusCode << [299, 400]
  }

  def "Cookies should be set on testClient during redirect"() {
    given:
    requestSpec { r -> r.redirects(10) }
    handlers {
      get {
        render(request.oneCookie("value") ?: "not set")
      }
      get(":cookie") {
        response.cookie("value", pathTokens.get("cookie"))
        redirect(responseCode, "/")
      }
    }

    when:
    get()

    then:
    response.body.text == "not set"

    when:
    get("/redirect")

    then:
    response.body.text == "redirect"

    when:
    get()

    then:
    response.body.text == "redirect"

    where:
    responseCode << [301, 302]
  }

  def "Response contains Set-Cookie header on redirect"() {
    given:
    handlers {
      get {
        render(request.oneCookie("value") ?: "not set")
      }
      get(":cookie") {
        response.cookie("value", pathTokens.get("cookie"))
        redirect(responseCode, "/")
      }
    }

    expect:
    getText() == "not set"

    when:
    get("/redirect")

    then:
    response.statusCode > 300 && response.statusCode < 303
    response.headers.get("Set-Cookie") == "value=redirect"

    where:
    responseCode << [301, 302]

  }

}
