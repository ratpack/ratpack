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

import ratpack.handling.internal.DefaultByContentSpec
import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

class ContentNegotiationSpec extends RatpackGroovyDslSpec {

  def "can content negotiate"() {
    when:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
          html {
            render "html"
          }
        }
      }
      get("noneRegistered") {
        byContent {}
      }
    }

    and:
    withAcceptHeader("application/json;q=0.5,text/html;q=1")
    then:
    text == "html"
    response.headers.get("Content-Type") == "text/html"
    response.statusCode == 200

    when:
    withAcceptHeader("application/json,text/html")
    then:
    text == "json"
    response.headers.get("Content-Type") == "application/json"
    response.statusCode == 200

    when:
    withAcceptHeader("*")
    then:
    text == "json"

    when:
    withAcceptHeader("*/*")
    then:
    text == "json"

    when:
    withAcceptHeader("text/*")
    then:
    text == "html"

    when:
    withAcceptHeader("")
    then:
    text == "json"

    when:
    resetRequest()
    // No Accept header
    then:
    text == "json"

    when:
    withAcceptHeader("some/nonsense")
    then:
    text == ""
    response.statusCode == 406

    then:
    getText("noneRegistered") == ""
    response.statusCode == 406
  }

  def "by accepts responder mime types"() {
    when:
    handlers {
      get {
        byContent {
          json { render "json" }
          xml { render "xml" }
          plainText { render "text" }
          html { render "html" }
        }
      }
    }

    and:
    withAcceptHeader("application/json")
    then:
    text == "json"

    when:
    withAcceptHeader("application/xml")
    then:
    text == "xml"

    when:
    withAcceptHeader("text/plain")
    then:
    text == "text"

    when:
    withAcceptHeader("text/html")
    then:
    text == "html"
  }

  @Unroll
  def "refuses invalid custom mime types (#mimeType)"() {
    when:
    new DefaultByContentSpec([:]).type(mimeType) {}

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message == message

    where:
    mimeType | message
    null     | "mimeType cannot be null"
    ""       | "mimeType cannot be a blank string"
    "*"      | "mimeType cannot include wildcards"
    "*/*"    | "mimeType cannot include wildcards"
    "text/*" | "mimeType cannot include wildcards"
  }

  def "default to 406 clientError when no handlers"() {
    when:
    handlers {
      get {
        byContent {}
      }
    }

    then:
    get().statusCode == 406
  }

  def "default to 406 clientError when content type not matched"() {
    when:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
        }
      }
    }

    and:
    withAcceptHeader("application/xml")
    then:
    get().statusCode == 406
  }

  def "responds with 406 clientError for invalid accept header values"() {
    given:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
        }
      }
    }

    when:
    withAcceptHeader("a")
    then:
    get().statusCode == 406

    when:
    withAcceptHeader("application/")
    then:
    get().statusCode == 406
  }

  def "can match against invalid header values"() {
    given:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
          type("abc") {
            render "abc"
          }
        }
      }
    }

    when:
    withAcceptHeader("abc")

    then:
    getText() == "abc"

    when:
    withAcceptHeader("application/")

    then:
    get().statusCode == 406
  }

  def "invalid content type parameter values are ignored"() {
    given:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
        }
      }
    }

    when:
    withAcceptHeader("application/json;q")
    then:
    text == "json"

    when:
    withAcceptHeader("application/json;q=afsdf")
    then:
    text == "json"
  }

  def "can register fallback content type"() {
    when:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
          noMatch("application/json")
        }
      }
    }

    and:
    withAcceptHeader("application/xml")
    then:
    text == "json"
    response.body.contentType.type == "application/json"
  }

  def "can register custom noMatch handler"() {
    when:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
          noMatch {
            response.contentType("text/html")
            render "custom"
          }
        }
      }
    }

    and:
    withAcceptHeader("application/xml")
    then:
    text == "custom"
    response.body.contentType.type == "text/html"
  }

  void withAcceptHeader(String acceptHeaderValue) {
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", acceptHeaderValue) }
  }
}
