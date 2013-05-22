package org.ratpackframework.path

import org.ratpackframework.test.DefaultRatpackSpec

import static org.ratpackframework.groovy.ClosureHandlers.exactPath
import static org.ratpackframework.groovy.ClosureHandlers.handler
import static org.ratpackframework.groovy.ClosureHandlers.path

class PathRoutingSpec extends DefaultRatpackSpec {

  def "can route by path"() {
    when:
    app {
      handlers {
        add path("abc") {
          add handler {
            response.send(get(PathBinding).boundTo)
          }
        }
      }
    }

    then:
    getText("abc/def") == "abc"
  }

  def "can route by nested path"() {
    when:
    app {
      handlers {
        add path("abc") {
          add path("def") {
            add handler {
              response.send(get(PathBinding).pastBinding)
            }
          }
        }
      }
    }

    then:
    getText("abc/def/ghi") == "ghi"
  }

  def "can route by path with tokens"() {
    when:
    app {
      handlers {
        add path(":a/:b/:c") {
          add handler {
            def pathContext = get(PathBinding)
            response.send("$pathContext.tokens - $pathContext.pastBinding")
          }
        }
      }
    }

    then:
    getText("1/2/3/4/5") == "[a:1, b:2, c:3] - 4/5"
  }

  def "can route by nested path with tokens"() {
    when:
    app {
      handlers {
        add path(":a/:b") {
          add path(":d/:e") {
            add handler {
              def pathContext = get(PathBinding)
              response.send("$pathContext.tokens - $pathContext.allTokens - $pathContext.pastBinding")
            }
          }
        }
      }
    }

    then:
    getText("1/2/3/4/5/6") == "[d:3, e:4] - [a:1, b:2, d:3, e:4] - 5/6"
  }

  def "can route by exact path with tokens"() {
    when:
    app {
      handlers {
        add handler(":a/:b/:c") {
          def pathContext = get(PathBinding)
          response.send("$pathContext.tokens - $pathContext.pastBinding")
        }
      }
    }

    then:
    get("1/2/3/4/5").statusCode == 404
    getText("1/2/3") == "[a:1, b:2, c:3] - "
  }

  def "can route by nested exact path with tokens"() {
    when:
    app {
      handlers {
        add path(":a/:b") {
          add handler(":d/:e") {
            def pathContext = get(PathBinding)
            response.send("$pathContext.tokens - $pathContext.allTokens - $pathContext.pastBinding")
          }
        }
      }
    }

    then:
    getText("1/2/3/4") == "[d:3, e:4] - [a:1, b:2, d:3, e:4] - "
    get("1/2/3/4/5/6").statusCode == 404
  }

}
