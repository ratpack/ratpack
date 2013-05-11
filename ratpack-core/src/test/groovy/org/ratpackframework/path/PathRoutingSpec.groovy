package org.ratpackframework.path

import org.ratpackframework.test.DefaultRatpackSpec

import static org.ratpackframework.groovy.ClosureHandlers.exactPath
import static org.ratpackframework.groovy.ClosureHandlers.handler
import static org.ratpackframework.groovy.ClosureHandlers.path

class PathRoutingSpec extends DefaultRatpackSpec {

  def "can route by path"() {
    when:
    app {
      routing {
        route path("abc") {
          route handler {
            response.send(get(PathContext).boundTo)
          }
        }
      }
    }

    then:
    urlGetText("abc/def") == "abc"
  }

  def "can route by nested path"() {
    when:
    app {
      routing {
        route path("abc") {
          route path("def") {
            route handler {
              response.send(get(PathContext).pastBinding)
            }
          }
        }
      }
    }

    then:
    urlGetText("abc/def/ghi") == "ghi"
  }

  def "can route by path with tokens"() {
    when:
    app {
      routing {
        route path(":a/:b/:c") {
          route handler {
            def pathContext = get(PathContext)
            response.send("$pathContext.tokens - $pathContext.pastBinding")
          }
        }
      }
    }

    then:
    urlGetText("1/2/3/4/5") == "[a:1, b:2, c:3] - 4/5"
  }

  def "can route by nested path with tokens"() {
    when:
    app {
      routing {
        route path(":a/:b") {
          route path(":d/:e") {
            route handler {
              def pathContext = get(PathContext)
              response.send("$pathContext.tokens - $pathContext.allTokens - $pathContext.pastBinding")
            }
          }
        }
      }
    }

    then:
    urlGetText("1/2/3/4/5/6") == "[d:3, e:4] - [a:1, b:2, d:3, e:4] - 5/6"
  }

  def "can route by exact path with tokens"() {
    when:
    app {
      routing {
        route exactPath(":a/:b/:c") {
          route handler {
            def pathContext = get(PathContext)
            response.send("$pathContext.tokens - $pathContext.pastBinding")
          }
        }
      }
    }

    then:
    urlGetConnection("1/2/3/4/5").responseCode == 404
    urlGetText("1/2/3") == "[a:1, b:2, c:3] - "
  }

  def "can route by nested exact path with tokens"() {
    when:
    app {
      routing {
        route path(":a/:b") {
          route exactPath(":d/:e") {
            route handler {
              def pathContext = get(PathContext)
              response.send("$pathContext.tokens - $pathContext.allTokens - $pathContext.pastBinding")
            }
          }
        }
      }
    }

    then:
    urlGetText("1/2/3/4") == "[d:3, e:4] - [a:1, b:2, d:3, e:4] - "
    urlGetConnection("1/2/3/4/5/6").responseCode == 404
  }

}
