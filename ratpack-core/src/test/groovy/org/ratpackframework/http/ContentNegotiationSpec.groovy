package org.ratpackframework.http

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

class ContentNegotiationSpec extends RatpackGroovyDslSpec {

  def "can content negotiate"() {
    when:
    app {
      handlers {
        get {
          accepts.
          type("application/json") {
            response.send "json"
          }.
          type("text/html") {
            response.send "html"
          }.
          send()
        }
        get("noneRegistered") {
          accepts.send()
        }
      }
    }

    then:
    request.header("Accept", "application/json;q=0.5,text/html;q=1")
    text == "html"
    response.header("Content-Type") == "text/html"
    response.statusCode == 200

    then:
    request.header("Accept", "application/json,text/html")
    text == "json"
    response.header("Content-Type") == "application/json"
    response.statusCode == 200

    then:
    request.header("Accept", "*")
    text == "json"

    then:
    request.header("Accept", "*/*")
    text == "json"

    then:
    request.header("Accept", "text/*")
    text == "html"

    then:
    request.header("Accept", "")
    text == "json"

    then:
    request.header("Accept", "some/nonsense")
    text == ""
    response.statusCode == 406

    then:
    getText("noneRegistered") == ""
    response.statusCode == 406
  }
}
