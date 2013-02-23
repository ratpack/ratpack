package org.ratpackframework.groovy

import org.ratpackframework.bootstrap.RatpackServer

class RatpackTest extends RatpackGroovySpec {

  def scriptClosure = {}

  def script(Closure script) {
    scriptClosure = script
  }

  def "can use static method"() {
    given:
    script {
      deployment.port = 0
    }
    templateFile("template.html") << "foo"
    ratpackFile << """
      get("/") { render "template.html" }
    """

    when:
    startApp()

    then:
    urlGetText("") == "foo"
  }

  @Override
  RatpackServer createApp() {
    Ratpack.ratpack(temporaryFolder.root, scriptClosure)
  }

}
