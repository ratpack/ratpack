package org.ratpackframework

import org.ratpackframework.groovy.render.GroovyTextRendererModule
import org.ratpackframework.test.DefaultRatpackSpec

class RequestMethodsSpec extends DefaultRatpackSpec {

  def "can get query params"() {
    given:
    modules { register(new GroovyTextRendererModule()) }

    routing {
      get("/") {
        text request.queryParams
      }
    }

    when:
    startApp()

    then:
    urlGetText() == "[:]"
    urlGetText("?a=b") == "[a:[b]]"
    urlGetText("?a[]=b&a[]=c&d=e") == "[a[]:[b, c], d:[e]]"
    urlGetText("?abc") == "[abc:[]]"
  }
}
