package org.ratpackframework.groovy

class BasicGroovyAppSpec extends RatpackGroovyAppSpec {

  def "can use special Groovy dsl"() {
    when:
    app {
      routing {
        get("a") {
          response.send "a handler"
        }
        post("b") {
          response.send "b handler"
        }
        path(":first") {
          get(":second") {
            response.send allPathTokens.toString()
          }
        }
      }
    }

    then:
    urlGetText("a") == "a handler"
    urlGetConnection("b").responseCode == 415
    urlPostText("b") == "b handler"
    urlGetText("1/2") == "[first:1, second:2]"
  }

}
