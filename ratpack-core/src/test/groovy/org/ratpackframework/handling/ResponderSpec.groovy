package org.ratpackframework.handling

import org.ratpackframework.groovy.handling.ClosureHandlers
import org.ratpackframework.test.DefaultRatpackSpec
import static org.ratpackframework.groovy.Util.with

/**
 * User: danielwoods
 * Date: 8/8/13
 */
class ResponderSpec extends DefaultRatpackSpec {

  def "by accepts responder mime types"() {
    setup:
    "setting up the handler chain"
    app {
      handlers {
        add Handlers.chain(
          [ClosureHandlers.get {
            with(accepts) {
              json      { response.send "json" }
              xml       { response.send "xml"  }
              plainText { response.send "text" }
              html      { response.send "html" }
            }
          }])
      }
    }

    when:
    "testing json"
    request.header("Accept", "application/json")

    then:
    "the json accept type should be invoked"
    text == "json"

    when:
    "testing xml"
    request.header("Accept", "application/xml")

    then:
    "the xml accept type should be invoked"
    text == "xml"

    when:
    "testing plainText"
    request.header("Accept", "text/plain")

    then:
    "the plain text accept type should be invoked"
    text == "text"

    when:
    "testing html"
    request.header("Accept", "text/html")

    then:
    "the plain text accept type should be invoked"
    text == "html"
  }
}
