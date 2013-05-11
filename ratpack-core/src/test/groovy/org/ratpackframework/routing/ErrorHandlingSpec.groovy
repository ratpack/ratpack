package org.ratpackframework.routing

import org.ratpackframework.error.ErrorHandlingContext
import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

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

  def "can handle errors on forked threads"() {
    given:
    def errorHandler = new ErrorHandlingContext() {
      void error(Exchange exchange, Exception exception) {
        exchange.response.send("Caught: $exception.message")
      }
    }

    when:
    app {
      routing {
        context(errorHandler) {
          get {
            withErrorHandling new Thread({
              throw new Exception("thrown in forked thread")
            })
          }
        }
      }
    }

    then:
    urlGetText() == "Caught: thrown in forked thread"
  }

}
