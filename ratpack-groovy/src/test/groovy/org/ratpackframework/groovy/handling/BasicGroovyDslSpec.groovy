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
    urlGetText("a") == "a handler"
    urlGetConnection("b").responseCode == 405
    urlPostText("b") == "b handler"
    urlGetText("1/2") == "[first:1, second:2]"
    urlGetText("foo/c/bar") == "[first:foo, second:bar]"
    urlGetText("foo.txt") == "bar"
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
    urlGetText("foo") == 'foo'
    urlGetText("bar") == 'bar'
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
    urlGetText("foo") == "common: get"
    urlPostText("foo") == "common: post"
    urlConnection("foo", "PUT").responseCode == 405
  }

}
