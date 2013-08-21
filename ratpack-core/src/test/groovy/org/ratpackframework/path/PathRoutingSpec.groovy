/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.path

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND


class PathRoutingSpec extends RatpackGroovyDslSpec {

  def "can route by prefix"() {
    when:
    app {
      handlers {
        prefix("abc") {
          handler {
            response.send(get(PathBinding).boundTo)
          }
        }
      }
    }

    then:
    getText("abc/def") == "abc"
    getText("abc/ghi") == "abc"
    getText("abc") == "abc"
    get("ab/def").statusCode == NOT_FOUND.code()
  }

  def "can route by nested prefix"() {
    when:
    app {
      handlers {
        prefix("abc") {
          prefix("def") {
            handler {
              response.send(get(PathBinding).pastBinding)
            }
          }
        }
      }
    }

    then:
    getText("abc/def/ghi") == "ghi"
    getText("abc/def/jkl") == "jkl"
    getText("abc/def") == ""
    get("abc/de/ghi").statusCode == NOT_FOUND.code()
  }

  def "can route by multi path prefix"() {
    when:
    app {
      handlers {
        prefix(["abc", "ghi"]) {
          handler {
            def binding = get(PathBinding)
            binding.boundTo.equals("abc") ? next() : response.send("handler1 - $binding.boundTo")
          }
        }

        prefix(["abc", "ghi", "mno"]) {
          prefix("def") {
            handler {
              def binding = get(PathBinding)
              response.send("handler2 - $binding.boundTo")
            }
          }
        }
      }
    }

    then:
    getText("abc/def/jkl") == "handler2 - def"
    getText("ghi/def/jkl") == "handler1 - ghi"
    getText("mno/def/jkl") == "handler2 - def"
  }

  def "can route by prefix with tokens"() {
    when:
    app {
      handlers {
        prefix(":a/:b/:c") {
          handler {
            def binding = get(PathBinding)
            response.send("$binding.tokens - $binding.pastBinding")
          }
        }
      }
    }

    then:
    getText("1/2/3/4/5") == "[a:1, b:2, c:3] - 4/5"
  }

  def "can route by nested prefix with tokens"() {
    when:
    app {
      handlers {
        prefix(":a/:b") {
          prefix(":d/:e") {
            handler {
              def binding = get(PathBinding)
              response.send("$binding.tokens - $binding.allTokens - $binding.pastBinding")
            }
          }
        }
      }
    }

    then:
    getText("1/2/3/4/5/6") == "[d:3, e:4] - [a:1, b:2, d:3, e:4] - 5/6"
  }

  def "can route by exact path"() {
    when:
    app {
      handlers {
        path("abc") {
          response.send(get(PathBinding).boundTo)
        }
      }
    }

    then:
    getText("abc") == "abc"
    get("abc/def").statusCode == NOT_FOUND.code()
    get("ab").statusCode == NOT_FOUND.code()
  }

  def "can route by nested exact path"() {
    when:
    app {
      handlers {
        prefix("abc") {
          path("def") {
            response.send(get(PathBinding).boundTo)
          }
        }
      }
    }

    then:
    getText("abc/def") == "def"
    get("abc/def/ghi").statusCode == NOT_FOUND.code()
    get("abc/de").statusCode == NOT_FOUND.code()
    get("abc").statusCode == NOT_FOUND.code()
  }

  def "can route by exact multi path"() {
    when:
    app {
      handlers {
        path(["abc", "def"]) {
          def binding = get(PathBinding)
          binding.boundTo.equals("abc") ? next() : response.send("handler1 - $binding.boundTo")
        }

        path("abc") {
          def binding = get(PathBinding)
          response.send("handler2 - $binding.boundTo")
        }
      }
    }

    then:
    getText("abc") == "handler2 - abc"
    getText("def") == "handler1 - def"
  }

  def "can route by exact path with tokens"() {
    when:
    app {
      handlers {
        path(":a/:b/:c") {
          def binding = get(PathBinding)
          response.send("$binding.tokens - $binding.pastBinding")
        }
      }
    }

    then:
    get("1/2/3/4/5").statusCode == NOT_FOUND.code()
    getText("1/2/3") == "[a:1, b:2, c:3] - "
  }

  def "can route by nested exact path with tokens"() {
    when:
    app {
      handlers {
        prefix(":a/:b") {
          path(":d/:e") {
            def binding = get(PathBinding)
            response.send("$binding.tokens - $binding.allTokens - $binding.pastBinding")
          }
        }
      }
    }

    then:
    getText("1/2/3/4") == "[d:3, e:4] - [a:1, b:2, d:3, e:4] - "
    get("1/2/3/4/5/6").statusCode == NOT_FOUND.code()
  }

  def "can use get handler"() {
    when:
    app {
      handlers {
        get {
          response.send("root")
        }

        get("a") {
          response.send("a")
        }
      }
    }

    then:
    getText() == "root"
    getText("a") == "a"
    get("a/b/c").statusCode == NOT_FOUND.code()
  }

  def "can use multi path get handler"() {
    when:
    app {
      handlers {
        get(["abc", "def"]) {
          def binding = get(PathBinding)
          binding.boundTo.equals("abc") ? next() : response.send("handler1 - $binding.boundTo")
        }

        get("abc") {
          def binding = get(PathBinding)
          response.send("handler2 - $binding.boundTo")
        }
      }
    }

    then:
    getText("abc") == "handler2 - abc"
    getText("def") == "handler1 - def"
  }

}
