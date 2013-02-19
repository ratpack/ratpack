package org.ratpackframework.groovy

class RoutingScriptSpec extends RatpackGroovySpec {

  def "is reloadable"() {
    given:
    ratpackFile << """
      get("/") {
        text "foo"
      }
    """

    when:
    startApp()

    then:
    urlGetText() == "foo"

    when:
    ratpackFile.text = """
      get("/") {
        text "bar"
      }
    """

    then:
    urlGetText() == "bar"
  }

  def "can disable reloading"() {
    given:
    config.routing.reloadable = false
    ratpackFile << """
      get("/") {
        text "foo"
      }
    """

    when:
    startApp()

    then:
    urlGetText() == "foo"

    when:
    ratpackFile.text = """
      get("/") {
        text "bar"
      }
    """

    then:
    urlGetText() == "foo"
  }

  def "app does not start when routes is invalid and reloading disabled"() {
    given:
    config.routing.reloadable = false
    ratpackFile << """
      s s da
    """

    when:
    startApp()

    then:
    thrown(Exception)
  }

  def "can redirect"() {
    given:
    ratpackFile << """
      get("/") {
        redirect "/foo"
      }
      get("/foo") {
        text "foo"
      }
    """

    when:
    startApp()

    then:
    urlGetText('') == "foo"
  }

}
