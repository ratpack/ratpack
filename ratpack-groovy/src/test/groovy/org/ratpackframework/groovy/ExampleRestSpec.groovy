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

package org.ratpackframework.groovy

import org.ratpackframework.test.internal.RatpackGroovyScriptAppSpec

class ExampleRestSpec extends RatpackGroovyScriptAppSpec {

  def "get by id"() {
    given:
    script """
      ratpack {
        handlers {
          get {
            response.send "Yeah!"
          }
        }
      }
    """

    when:
    get()

    then:
    response.statusCode == 200
    response.body.asString() == "Yeah!"
  }

  def "can verify json"() {
    given:
    script """
      import groovy.json.JsonOutput
      import groovy.json.JsonSlurper

      ratpack {
        handlers {
          post {
            if (!request.contentType.json) {
              clientError(415)
              return
            }

            respond byContent.
              type("application/json") {
                def json = new JsonSlurper().parseText(request.text)
                def value = json.value
                response.send "application/json", JsonOutput.toJson([value: value * 2])
              }
          }
        }
      }
    """

    when:
    request.with {
      contentType "text/plain"
      body "foo"
    }

    post()

    then:
    response.statusCode == 415

    when:
    request.with {
      header "Accept", "application/json"
      contentType "application/json"
      body '{"value": 1}'
    }

    post()

    then:
    response.statusCode == 200
    response.jsonPath().value == 2
  }

  def "can read request body"() {
    given:
    script """
      ratpack {
        handlers {
          post {
            response.send(request.text)
          }
        }
      }
    """

    when:
    request.with {
      body "foo"
    }

    post()

    then:
    response.statusCode == 200
    response.body.asString() == "foo"
  }

}
