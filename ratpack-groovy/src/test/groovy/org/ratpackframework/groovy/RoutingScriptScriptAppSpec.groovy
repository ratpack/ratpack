package org.ratpackframework.groovy

class RoutingScriptScriptAppSpec extends RatpackGroovyScriptAppSpec {

  def "is reloadable"() {
    given:
    reloadable = true

    ratpackFile << """
      routing {
        get("/") {
          text "foo"
        }
      }
    """

    when:
    startApp()

    then:
    urlGetText() == "foo"

    when:
    ratpackFile.text = """
      routing {
        get("/") {
          text "bar"
        }
      }
    """

    then:
    urlGetText() == "bar"
  }

  def "can disable reloading"() {
    given:
    reloadable = false

    ratpackFile << """
      routing {
        get("/") {
          text "foo"
        }
      }
    """

    when:
    startApp()

    then:
    urlGetText() == "foo"

    when:
    ratpackFile.text = """
      routing {
        get("/") {
          text "bar"
        }
      }
    """

    then:
    urlGetText() == "foo"
  }

  def "app does not start when routes is invalid and reloading disabled"() {
    given:
    reloadable = false

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
      routing {
        get("/") {
          redirect "/foo"
        }
        get("/foo") {
          text "foo"
        }
      }
    """

    when:
    startApp()

    then:
    urlGetText('') == "foo"
  }

}
