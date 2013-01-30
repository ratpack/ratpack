package org.ratpackframework

class SessionSpec extends RatpackSpec {

  def "can use session"() {
    given:
    ratpackFile << """
      get("/:v") {
        renderString it.session.getId()
      }
    """

    when:
    startApp()

    then:
    urlGetText("a") == urlGetText("b")
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
    startApp()

    and:
    urlGetText("set/foo") == "foo"

    then:
    urlGetText() == "foo"
  }

}
