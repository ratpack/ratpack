package org.ratpackframework

class CookiesSpec extends RatpackSpec {

  def "can get and set cookies"() {
    given:
    ratpackFile << """
      get("/get/:name") { r ->
        text request.cookies.find { it.name == r.urlParams.name }.value
      }

      get("/set/:name/:value") {
        cookie(it.urlParams.name, it.urlParams.value)
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

  }

}
