package org.ratpackframework.handling

import org.ratpackframework.groovy.handling.ClosureHandlers
import org.ratpackframework.test.DefaultRatpackSpec

class HandlersSpec extends DefaultRatpackSpec {

  def "empty chain handler"() {
    when:
    app {
      handlers {
        add Handlers.chain()
      }
    }

    then:
    get().statusCode == 404
  }

  def "single chain handler"() {
    when:
    app {
      handlers {
        add Handlers.chain(ClosureHandlers.get { response.send("foo") })
      }
    }

    then:
    text == "foo"
  }

  def "multi chain handler"() {
    when:
    app {
      handlers {
        add Handlers.chain(
            ClosureHandlers.get("a") { response.send("foo") },
            ClosureHandlers.get("b") { response.send("bar") }
        )
      }
    }

    then:
    getText("a") == "foo"
    getText("b") == "bar"
  }
}
