package org.ratpackframework

class RequestMethodsSpec extends RatpackSpec {

  def "can get query params"() {
    given:
    ratpackFile << """
      get("/") {
        text request.queryParams
      }
    """

    when:
    startApp()

    then:
    urlGetText() == "[:]"
    urlGetText("?a=b") == "[a:[b]]"
    urlGetText("?a[]=b&a[]=c&d=e") == "[a[]:[b, c], d:[e]]"
    urlGetText("?abc") == "[abc:[]]"
  }
}
