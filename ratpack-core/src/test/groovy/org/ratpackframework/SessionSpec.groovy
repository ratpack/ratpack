package org.ratpackframework

import spock.lang.Ignore

@Ignore
class SessionSpec extends RatpackSpec {

  def "can use session"() {
    given:
    ratpackFile << """
      get("/:v") {
        renderString it.session.getId()
      }
    """

    when:
    app.start()

    then:
    urlText("a") == urlText("b")
  }

  def "can store session vars"() {
    given:
    ratpackFile << """
      get("/") {
        renderString it.session.value
      }
      get("/set/:value") {
        it.session.value = it.urlParams.value
        renderString it.session.value
      }
    """

    when:
    app.start()

    and:
    urlText("set/foo") == "foo"

    then:
    urlText() == "foo"
  }

}
