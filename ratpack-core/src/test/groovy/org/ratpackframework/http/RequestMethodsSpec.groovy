package org.ratpackframework.http

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

class RequestMethodsSpec extends RatpackGroovyDslSpec {

  def "can get query params"() {
    when:
    app {
      handlers {
        get("") {
          response.send request.queryParams.toString()
        }
      }
    }

    then:
    getText() == "[:]" && resetRequest()
    getText("?a=b") == "[a:[b]]" && resetRequest()
    request {
      queryParam "a[]", "b", "c"
      queryParam "d", "e"
    }
    getText() == "[a[]:[b, c], d:[e]]" && resetRequest()
    getText("?abc") == "[abc:[]]" && resetRequest()
  }
}
