package org.ratpackframework.test.groovy

class BasicGroovyScriptAppSpec extends RatpackGroovyScriptAppSpec {

  def "can use script app"() {
    given:
    compileStatic = true

    when:
    app {
      script """
        ratpack {
          routing {
            get("") {
              getResponse().send("foo")
            }
          }
        }
      """
    }

    then:
    urlGetText("") == "foo"
  }
}
