package org.ratpackframework.http

import org.ratpackframework.groovy.RatpackGroovyDslSpec

class RequestMethodsSpec extends RatpackGroovyDslSpec {

  def "can get query params"() {
    when:
    app {
      routing {
        get("") {
          response.send request.queryParams.toString()
        }
      }
    }

    then:
    urlGetText() == "[:]"
    urlGetText("?a=b") == "[a:[b]]"
    urlGetText("?a[]=b&a[]=c&d=e") == "[a[]:[b, c], d:[e]]"
    urlGetText("?abc") == "[abc:[]]"
  }
}
