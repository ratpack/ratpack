package org.ratpackframework

class SessionSpec extends RatpackSpec {

  def "can use session"() {
    given:
    ratpackFile << """
      get("/:v") {
        renderText it.session.getId()
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
        renderText it.session.value
      }
      get("/set/:value") {
        it.session.value = it.urlParams.value
        renderText it.session.value
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
