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

import io.netty.handler.codec.http.HttpHeaderValues
import ratpack.error.ServerErrorHandler
import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

class ContentNegotiationSpec extends RatpackGroovyDslSpec {

  def "can content negotiate accept header '#acceptHeader' to '#contentTypeHeader'"() {
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
          plainText {
            render "text"
          }
        }
      }
    }

    and:
    withAcceptHeader(acceptHeader)
    then:
    text == body
    response.headers.get("Content-Type") == contentTypeHeader
    response.statusCode == 200

    where:
    acceptHeader                           | contentTypeHeader          | body
    "application/json;q=0.5,text/html;q=1" | "text/html;charset=UTF-8"  | "html"
    "application/json,text/html"           | "application/json"         | "json"
    "*"                                    | "application/json"         | "json"
    "*/*"                                  | "application/json"         | "json"
    "text/*"                               | "text/html;charset=UTF-8"  | "html"
    "text/plain"                           | "text/plain;charset=UTF-8" | "text"
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

  def "Accepts valid custom mime types (#mimeType"(CharSequence mimeType, String expectedOutput) {
    given:
    handlers {
        get {
            byContent {
                type(HttpHeaderValues.APPLICATION_OCTET_STREAM) {
                    render "octect-stream"
                  }
                type("application/x-protobuf") {
                    render "x-protobuf"
                  }
              }
          }
      }

      when:
    withAcceptHeader(mimeType.toString())

      then:
    text == expectedOutput

      where:
    mimeType                                  || expectedOutput
    HttpHeaderValues.APPLICATION_OCTET_STREAM || "octect-stream"
    "application/x-protobuf"                  || "x-protobuf"
  }

  def "refuses invalid custom mime types (#mimeType)"(String mimeType, String message) {
    when:
    bindings {
      bindInstance(ServerErrorHandler, new SimpleErrorHandler())
    }

    handlers {
      get {
        byContent {
          type(mimeType) {}
        }
      }
    }

    then:
    text == "java.lang.IllegalArgumentException: $message".toString()

    where:
    mimeType | message
    null     | "mimeType cannot be null"
    ""       | "mimeType cannot be a blank string"
    "*"      | "mimeType cannot include wildcards"
    "*/*"    | "mimeType cannot include wildcards"
    "text/*" | "mimeType cannot include wildcards"
  }

  def "default to 406 clientError when no blocks registered"() {
    when:
    handlers {
      get {
        byContent {}
      }
    }

    then:
    get().statusCode == 406
  }

  def "default to 406 clientError when content type (#mimeType) not matched"(String mimeType) {
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
    withAcceptHeader(mimeType)
    then:
    get().statusCode == 406

    where:
    mimeType << ["application/xml", "some/nonsense"]
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

  def "default noMatch behavior is 406 error"() {
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

  def "can register fallback noMatch content type"() {
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

  def "noMatch for a type without a matching block results in an error"() {
    when:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
          noMatch("some/nonsense")
        }
      }
    }

    and:
    withAcceptHeader("application/xml")
    then:
    get().statusCode == 500
  }

  def "can register custom noMatch block"() {
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

  def "default unspecified behavior is first block"() {
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

    then:
    text == "json"
    response.body.contentType.type == "application/json"
  }

  def "treats empty accept headers as unspecified"() {
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
    withAcceptHeader("")
    then:
    text == "json"
    response.body.contentType.type == "application/json"
  }

  def "can register fallback unspecified content type"() {
    when:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
          xml {
            render "xml"
          }
          unspecified("application/xml")
        }
      }
    }

    then:
    text == "xml"
    response.body.contentType.type == "application/xml"
  }

  def "unspecified for a type without a matching block results in an error"() {
    when:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
          unspecified("some/nonsense")
        }
      }
    }

    then:
    get().statusCode == 500
  }

  def "can register custom unspecified block"() {
    when:
    handlers {
      get {
        byContent {
          json {
            render "json"
          }
          unspecified {
            response.contentType("text/html")
            render "custom"
          }
        }
      }
    }

    then:
    text == "custom"
    response.body.contentType.type == "text/html"
  }

  void withAcceptHeader(String acceptHeaderValue) {
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", acceptHeaderValue) }
  }
}
