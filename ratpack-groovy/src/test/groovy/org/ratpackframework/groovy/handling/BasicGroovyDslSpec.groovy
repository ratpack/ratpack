package org.ratpackframework.groovy.handling

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

class BasicGroovyDslSpec extends RatpackGroovyDslSpec {

  def "can use special Groovy dsl"() {
    given:
    file("public/foo.txt") << "bar"

    when:
    app {
      handlers {
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
          handler("c/:second") {
            response.send allPathTokens.toString()
          }
        }
        assets("public")
      }
    }

    then:
    getText("a") == "a handler"
    get("b").statusCode == 405
    postText("b") == "b handler"
    getText("1/2") == "[first:1, second:2]"
    getText("foo/c/bar") == "[first:foo, second:bar]"
    getText("foo.txt") == "bar"
  }

  def "can use file method to access file contextual"() {
    given:
    file("foo/file.txt") << "foo"
    file("bar/file.txt") << "bar"

    when:
    app {
      handlers {
        fileSystem("foo") {
          get("foo") {
            response.send file("file.txt").text
          }
        }
        fileSystem("bar") {
          get("bar") {
            response.send file("file.txt").text
          }
        }
      }
    }

    then:
    getText("foo") == 'foo'
    getText("bar") == 'bar'
  }

  def "can use method chain"() {
    when:
    app {
      handlers {
        handler("foo") {
          def prefix = "common"

          methods.get {
            response.send("$prefix: get")
          }.post {
            response.send("$prefix: post")
          }.send()
        }
      }
    }

    then:
    getText("foo") == "common: get"
    postText("foo") == "common: post"
    put("foo").statusCode == 405
  }

}
