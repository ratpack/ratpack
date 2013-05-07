package org.ratpackframework

import org.ratpackframework.groovy.RatpackGroovyDslSpec

class ErrorHandlingSpec extends RatpackGroovyDslSpec {

  def "handles 404"() {
    when:
    app {}

    then:
    urlGetConnection("foo").responseCode == 404
  }

  def "handles internal error"() {
    when:
    app {
      routing {
        get("") { throw new RuntimeException('error here') }
      }
    }

    then:
    urlGetConnection().responseCode == 500
  }

}
