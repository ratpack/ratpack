package org.ratpackframework

class ErrorHandlingSpec extends RatpackSpec {

  def "handles 404"() {
    when:
    startApp()

    then:
    urlConnection("foo").responseCode == 404
  }

  def "handles internal error"() {
    given:
    ratpackFile << """
      get("/") { throw new RuntimeException('error here') }
    """

    when:
    startApp()

    then:
    errorText().contains 'error here'
  }
}
