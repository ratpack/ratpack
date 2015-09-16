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

package ratpack.error

import ratpack.test.internal.RatpackGroovyDslSpec

class DevelopmentErrorHandlerSpec extends RatpackGroovyDslSpec {

  def "debug error handler prints html info if client wants html"() {
    given:
    def e = new RuntimeException("!")
    requestSpec { it.headers.add("Accept", "text/html;q=1,text/plain;q=0.9") }

    when:
    serverConfig { development(true) }
    handlers {
      get("client") { clientError(404) }
      get("server") { error(e) }
    }

    then:
    with(get("client")) {
      statusCode == 404
      body.text.startsWith("<!DOCTYPE html>")
      body.contentType.html
    }

    with(get("server")) {
      statusCode == 500
      body.text.startsWith("<!DOCTYPE html>")
      body.contentType.html
    }
  }

  def "debug error handler prints text if no content preference"() {
    given:
    def e = new RuntimeException("!")

    when:
    serverConfig { development(true) }
    handlers {
      get("client") { clientError(404) }
      get("server") { error(e) }
    }

    then:
    with(get("client")) {
      statusCode == 404
      body.text.startsWith("Client error 404")
      body.contentType.text
    }

    with(get("server")) {
      statusCode == 500
      body.text.startsWith(RuntimeException.name)
      body.contentType.text
    }
  }

  def "debug error handler prints text if content type not matched"() {
    given:
    def e = new RuntimeException("!")
    requestSpec { it.headers.add("Accept", "application/json") }

    when:
    serverConfig { development(true) }
    handlers {
      get("client") { clientError(404) }
      get("server") { error(e) }
    }

    then:
    with(get("client")) {
      statusCode == 404
      body.text.startsWith("Client error 404")
      body.contentType.text
    }

    with(get("server")) {
      statusCode == 500
      body.text.startsWith(RuntimeException.name)
      body.contentType.text
    }
  }
}
