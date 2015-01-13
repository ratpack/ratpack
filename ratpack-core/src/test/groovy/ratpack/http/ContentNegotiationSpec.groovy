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
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/json;q=0.5,text/html;q=1") }
    then:
    text == "html"
    response.headers.get("Content-Type") == "text/html"
    response.statusCode == 200

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/json,text/html") }
    then:
    text == "json"
    response.headers.get("Content-Type") == "application/json"
    response.statusCode == 200

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "*") }
    then:
    text == "json"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "*/*") }
    then:
    text == "json"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "text/*") }
    then:
    text == "html"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "") }
    then:
    text == "json"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "some/nonsense") }
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
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/json") }
    then:
    text == "json"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/xml") }
    then:
    text == "xml"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "text/plain") }
    then:
    text == "text"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "text/html") }
    then:
    text == "html"
  }

  @Unroll
  def "supports #typeDecl media type declarations"() {
    when:
    handlers {
      get {
        byContent {
          json { render "json" }
          html { render "html" }
          type(typeDecl) { render "wildcard" }
        }
      }
    }

    and:
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/json") }
    then:
    text == "json"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "text/html") }
    then:
    text == "html"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/xml") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "text/plain") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/*") }
    then:
    text == "json"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "text/*") }
    then:
    text == "html"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "image/*") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "*") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "*/*") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "") }
    then:
    text == "json"

    where:
    typeDecl << ["*", "*/*"]
  }

  @Unroll
  def "supports 'allTypes' media type declarations"() {
    when:
    handlers {
      get {
        byContent {
          json { render "json" }
          html { render "html" }
          allTypes { render "wildcard" }
        }
      }
    }

    and:
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/json") }
    then:
    text == "json"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "text/html") }
    then:
    text == "html"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/xml") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "text/plain") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "application/*") }
    then:
    text == "json"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "text/*") }
    then:
    text == "html"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "image/*") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "*") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "*/*") }
    then:
    text == "wildcard"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec -> requestSpec.headers.add("Accept", "") }
    then:
    text == "json"
  }
}
