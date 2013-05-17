package org.ratpackframework.http

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

class CookiesSpec extends RatpackGroovyDslSpec {

  def "can get and set cookies"() {
    given:
    app {
      handlers {
        get("get/:name") {
          response.send request.oneCookie(pathTokens.name) ?: "null"
        }

        get("set/:name/:value") {
          response.cookie(pathTokens.name, pathTokens.value)
          response.send()
        }

        get("clear/:name") {
          response.expireCookie(pathTokens.name)
          response.send()
        }
      }
    }

    when:
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
