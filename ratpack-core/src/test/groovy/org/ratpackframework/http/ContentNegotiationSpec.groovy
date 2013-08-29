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

package org.ratpackframework.http

import org.ratpackframework.test.internal.RatpackGroovyDslSpec

class ContentNegotiationSpec extends RatpackGroovyDslSpec {

  def "can content negotiate"() {
    when:
    app {

      handlers {
        get {
          respond byContent.
            type("application/json") {
              response.send "json"
            }.
            type("text/html") {
              response.send "html"
            }
        }
        get("noneRegistered") {
          respond byContent
        }
      }
    }

    then:
    request.header("Accept", "application/json;q=0.5,text/html;q=1")
    text == "html"
    response.header("Content-Type") == "text/html;charset=UTF-8"
    response.statusCode == 200

    then:
    resetRequest()
    request.header("Accept", "application/json,text/html")
    text == "json"
    response.header("Content-Type") == "application/json"
    response.statusCode == 200

    then:
    resetRequest()
    request.header("Accept", "*")
    text == "json"

    then:
    resetRequest()
    request.header("Accept", "*/*")
    text == "json"

    then:
    resetRequest()
    request.header("Accept", "text/*")
    text == "html"

    then:
    resetRequest()
    request.header("Accept", "")
    text == "json"

    then:
    resetRequest()
    request.header("Accept", "some/nonsense")
    text == ""
    response.statusCode == 406

    then:
    getText("noneRegistered") == ""
    response.statusCode == 406
  }

  def "by accepts responder mime types"() {
    when:
    app {
      handlers {
        get {
          respond byContent.
            json { response.send "json" }.
            xml { response.send "xml" }.
            plainText { response.send "text" }.
            html { response.send "html" }
        }
      }
    }

    then:
    request.header("Accept", "application/json")
    text == "json"

    then:
    resetRequest()
    request.header("Accept", "application/xml")
    text == "xml"

    then:
    resetRequest()
    request.header("Accept", "text/plain")
    text == "text"

    then:
    resetRequest()
    request.header("Accept", "text/html")
    text == "html"
  }
}
