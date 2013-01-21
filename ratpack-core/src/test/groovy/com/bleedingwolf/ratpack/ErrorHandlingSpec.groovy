package com.bleedingwolf.ratpack

class ErrorHandlingSpec extends RatpackSpec {

  def "handles 404"() {
    when:
    app.start()

    then:
    urlConnection("foo").responseCode == 404
  }

  def "handles internal error"() {
    given:
    ratpackFile << """
      get("/") { throw new RuntimeException('error here') }
    """

    when:
    app.start()

    then:
    errorText().contains 'error here'
  }
}
