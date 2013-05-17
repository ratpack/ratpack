package org.ratpackframework.groovy.handling

import org.ratpackframework.test.groovy.RatpackGroovyScriptAppSpec

class BasicGroovyScriptAppSpec extends RatpackGroovyScriptAppSpec {

  def "can use script app"() {
    given:
    compileStatic = true

    when:
    app {
      script """
        ratpack {
          handlers {
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
