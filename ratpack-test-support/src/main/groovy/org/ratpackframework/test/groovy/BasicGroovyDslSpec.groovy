package org.ratpackframework.test.groovy

class BasicGroovyDslSpec extends RatpackGroovyDslSpec {

  def "can use special Groovy dsl"() {
    given:
    file("public/foo.txt") << "bar"

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
          handler("c/:second", ["get", "post"] as List) {
            response.send allPathTokens.toString()
          }
        }
        assets("public")
      }
    }

    then:
    urlGetText("a") == "a handler"
    urlGetConnection("b").responseCode == 415
    urlPostText("b") == "b handler"
    urlGetText("1/2") == "[first:1, second:2]"
    urlGetText("foo/c/bar") == "[first:foo, second:bar]"
    urlGetText("foo.txt") == "bar"
  }

}
