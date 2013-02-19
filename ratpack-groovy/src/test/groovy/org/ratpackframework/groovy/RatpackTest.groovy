package org.ratpackframework.groovy

import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.test.RatpackSpec

class RatpackTest extends RatpackSpec {

  def scriptClosure = {}

  def script(Closure script) {
    scriptClosure = script
  }

  def "can use static method"() {
    given:
    file("templates/template.html") << "foo"
    script {
      routing {
        get("/") { render "template.html" }
      }
    }

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
