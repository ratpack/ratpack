package org.ratpackframework

import org.ratpackframework.test.DefaultRatpackSpec

class CookiesSpec extends DefaultRatpackSpec {

  def "can get and set cookies"() {
    given:
    routing {
      get("/get/:name") {
        text request.oneCookie(request.pathParams.name)
      }

      get("/set/:name/:value") {
        cookie(it.pathParams.name, it.pathParams.value)
        end()
      }

      get("/clear/:name") {
        expireCookie(it.pathParams.name)
        end()
      }
    }

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
