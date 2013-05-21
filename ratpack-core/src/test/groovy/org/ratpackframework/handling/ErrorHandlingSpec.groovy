package org.ratpackframework.handling

import org.ratpackframework.error.ServerErrorHandler
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
      handlers {
        get("") { throw new RuntimeException('error here') }
      }
    }

    then:
    urlGetConnection().responseCode == 500
  }

  def "can handle errors on forked threads"() {
    given:
    def errorHandler = new ServerErrorHandler() {
      void error(Exchange exchange, Exception exception) {
        exchange.response.send("Caught: $exception.message")
      }
    }

    when:
    app {
      handlers {
        context(ServerErrorHandler, errorHandler) {
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
