package org.ratpackframework.path

import org.ratpackframework.test.DefaultRatpackSpec

import static org.ratpackframework.groovy.ClosureHandlers.*

class PathAndMethodRoutingSpec extends DefaultRatpackSpec {

  def "can use path and method routes"() {
    when:
    app {
      routing {
        route get("a/b/c") {
          response.send request.query
        }
        route path(":a/:b") {
          route handler(":c/:d", ["post", "put"] as List) {
            if (request.method.post) {
              response.send allPathTokens.toString()
            } else {
              response.send allPathTokens.collectEntries { [it.key.toUpperCase(), it.value.toUpperCase()] }.toString()
            }
          }
        }
      }
    }

    then:
    urlGetText("a/b/c?foo=bar") == "foo=bar"
    urlPostText("1/2/3/4") == "[a:1, b:2, c:3, d:4]"
    urlConnection("5/6/7/8", "PUT").inputStream.text == "[A:5, B:6, C:7, D:8]"
  }
}
