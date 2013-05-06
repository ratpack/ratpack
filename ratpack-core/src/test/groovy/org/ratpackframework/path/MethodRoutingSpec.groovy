package org.ratpackframework.path

import org.ratpackframework.test.DefaultRatpackSpec

import static org.ratpackframework.groovy.ClosureHandlers.handler
import static org.ratpackframework.groovy.ClosureHandlers.method

class MethodRoutingSpec extends DefaultRatpackSpec {

  def "can route by method"() {
    when:
    app {
      routing {
        route method(["get", "pOsT"]) {
          route handler {
            response.send request.method
          }
        }
      }
    }

    then:
    urlGetText("/") == "GET"
    urlPostText("/") == "POST"
    urlConnection("/", "PUT").responseCode == 415
  }

}
