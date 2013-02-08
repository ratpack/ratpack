package org.ratpackframework

class ErrorHandlingSpec extends RatpackSpec {

  def "handles 404"() {
    when:
    startApp()

    then:
    urlGetConnection("foo").responseCode == 404
  }

  def "handles internal error"() {
    given:
    ratpackFile << """
      get("/") { throw new RuntimeException('error here') }
    """

    when:
    startApp()

    then:
    errorGetText().contains 'error here'
  }

  def "can use wrap error"() {
    given:
    ratpackFile << """
      get("/") { request, response ->
        Thread.start {
          response.error(new Exception("bang!"))
        }
      }
    """

    when:
    startApp()

    then:
    errorGetText().contains 'bang!'
  }
}
