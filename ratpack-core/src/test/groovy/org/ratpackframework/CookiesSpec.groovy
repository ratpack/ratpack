package org.ratpackframework

class CookiesSpec extends RatpackSpec {

  def "can get and set cookies"() {
    given:
    ratpackFile << """
      get("/get/:name") {
        text request.oneCookie(request.urlParams.name)
      }

      get("/set/:name/:value") {
        cookie(it.urlParams.name, it.urlParams.value)
        end()
      }

      get("/clear/:name") {
        expireCookie(it.urlParams.name)
        end()
      }
    """

    when:
    startApp()
    urlGetText("set/a/1")

    then:
    urlGetText("get/a") == "1"

    when:
    urlGetText("set/a/2")
    urlGetText("set/b/1")

    then:
    urlGetText("get/a") == "2"
    urlGetText("get/b") == "1"

    when:
    urlGetText("clear/a")

    then:
    urlGetText("get/a") == "null"
  }

}
