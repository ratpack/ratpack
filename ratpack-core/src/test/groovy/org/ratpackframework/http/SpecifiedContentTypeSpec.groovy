package org.ratpackframework.http

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec
import spock.lang.Issue

import static io.netty.handler.codec.http.HttpResponseStatus.OK

@Issue("https://github.com/ratpack/ratpack/issues/127")
class SpecifiedContentTypeSpec extends RatpackGroovyDslSpec {

  def "content type can be specified by handler"() {
    given:
    app {
      handlers {
        get("path") {
          response.contentType mimeType
          response.send content
        }
      }
    }

    when:
    get("path")

    then:
    with(response) {
      statusCode == OK.code()
      response.body.asString() == content
      response.contentType == "$mimeType;charset=UTF-8"
    }

    where:
    content | mimeType
    "x,y,z" | "text/csv"
  }

}
