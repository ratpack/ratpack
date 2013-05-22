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
    getText("set/a/1")

    then:
    getText("get/a") == "1"

    when:
    getText("set/a/2")
    getText("set/b/1")

    then:
    getText("get/a") == "2"
    getText("get/b") == "1"

    when:
    getText("clear/a")

    then:
    getText("get/a") == "null"
  }

}
