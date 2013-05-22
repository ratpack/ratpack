package org.ratpackframework.test.rest

class ExampleRestSpec extends RestSpecSupportFixture {

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

import static org.ratpackframework.groovy.RatpackScript.ratpack

      ratpack {
        handlers {
          post {
            if (!request.contentType.json) {
              clientError(415)
              return
            }

            if (request.getHeader("Accept") != "application/json") {
              clientError(406)
              return
            }

            def json = new JsonSlurper().parseText(request.text)
            def value = json.value
            response.send "application/json", JsonOutput.toJson([value: value * 2])
          }
        }
      }
    """

    when:
    request {
      contentType "text/plain"
      body "foo"
    }

    post()

    then:
    response.statusCode == 415

    when:
    request {
      header "Accept", "application/json"
      contentType "application/json"
      body '{"value": 1}'
    }

    post()

    then:
    response.statusCode == 200
    response.jsonPath().value == 2
  }

}
