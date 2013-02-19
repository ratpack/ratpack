package org.ratpackframework

import org.ratpackframework.test.DefaultRatpackSpec

class ErrorHandlingSpec extends DefaultRatpackSpec {

  def "handles 404"() {
    when:
    startApp()

    then:
    urlGetConnection("foo").responseCode == 404
  }

  def "handles internal error"() {
    given:
    routing {
      get("/") { throw new RuntimeException('error here') }
    }

    when:
    startApp()

    then:
    urlGetConnection().responseCode == 500
  }

  def "can use wrap error"() {
    given:
    routing {
      get("/") { request, response ->
        Thread.start {
          response.error(new Exception("bang!"))
        }
      }
    }

    when:
    startApp()

    then:
    urlGetConnection().responseCode == 500
  }
}
