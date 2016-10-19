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

package ratpack.path

import ratpack.test.internal.RatpackGroovyDslSpec

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND

class PathRoutingSpec extends RatpackGroovyDslSpec {

  def "can route by prefix"() {
    when:
    handlers {
      prefix("abc") {
        all {
          render(pathBinding.boundTo)
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
    handlers {
      prefix("abc") {
        prefix("def") {
          all {
            render(pathBinding.pastBinding)
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

  def "can route by prefix with tokens"() {
    when:
    handlers {
      prefix(":a/:b/:c") {
        all {
          def binding = pathBinding
          render("$binding.tokens - $binding.pastBinding")
        }
      }
    }

    then:
    getText("1/2/3/4/5") == "[a:1, b:2, c:3] - 4/5"
  }

  def "can route by nested prefix with tokens"() {
    when:
    handlers {
      prefix(":a/:b") {
        prefix(":d/:e") {
          all {
            def binding = pathBinding
            render "$binding.tokens - $binding.allTokens - $binding.pastBinding"
          }
        }
      }
    }

    then:
    getText("1/2/3/4/5/6") == "[d:3, e:4] - [a:1, b:2, d:3, e:4] - 5/6"
  }

  def "can route by exact path"() {
    when:
    handlers {
      path("abc") {
        def binding = pathBinding
        render(binding.boundTo)
      }
    }

    then:
    getText("abc") == "abc"
    get("abc/def").statusCode == NOT_FOUND.code()
    get("ab").statusCode == NOT_FOUND.code()
  }

  def "can route by nested exact path"() {
    when:
    handlers {
      prefix("abc") {
        path("def") {
          render(pathBinding.boundTo)
        }
      }
    }

    then:
    getText("abc/def") == "def"
    get("abc/def/ghi").statusCode == NOT_FOUND.code()
    get("abc/de").statusCode == NOT_FOUND.code()
    get("abc").statusCode == NOT_FOUND.code()
  }

  def "can route by exact path with tokens"() {
    when:
    handlers {
      path(":a/:b/:c") {
        def binding = pathBinding
        render("$binding.tokens - $binding.pastBinding")
      }
    }

    then:
    get("1/2/3/4/5").statusCode == NOT_FOUND.code()
    getText("1/2/3") == "[a:1, b:2, c:3] - "
  }

  def "can route by nested exact path with tokens"() {
    when:
    handlers {
      prefix(":a/:b") {
        path(":d/:e") {
          def binding = pathBinding
          render("$binding.tokens - $binding.allTokens - $binding.pastBinding")
        }
      }
    }

    then:
    getText("1/2/3/4") == "[d:3, e:4] - [a:1, b:2, d:3, e:4] - "
    get("1/2/3/4/5/6").statusCode == NOT_FOUND.code()
  }

  def "can route by nested exact path with regular expression tokens"() {
    when:
    handlers {
      prefix(":a:[a-c]{1,3}/:b") {
        path(":d/:e?:4") {
          def binding = pathBinding
          render("$binding.tokens - $binding.allTokens - $binding.pastBinding")
        }
      }
    }

    then:
    getText("a/2/3/4") == "[d:3, e:4] - [a:a, b:2, d:3, e:4] - "
    getText("aa/2/3/4") == "[d:3, e:4] - [a:aa, b:2, d:3, e:4] - "
    getText("aaa/2/3/4") == "[d:3, e:4] - [a:aaa, b:2, d:3, e:4] - "
    getText("b/2/3/4") == "[d:3, e:4] - [a:b, b:2, d:3, e:4] - "
    getText("c/2/3/4") == "[d:3, e:4] - [a:c, b:2, d:3, e:4] - "

    getText("c/2/3") == "[d:3] - [a:c, b:2, d:3] - "

    get("d/2/3/4").statusCode == NOT_FOUND.code()
    get("1/2/3/4").statusCode == NOT_FOUND.code()
    get("aaaa/2/3/4").statusCode == NOT_FOUND.code()
    get("a/2/3/5").statusCode == NOT_FOUND.code()
  }

  def "can route by nested exact path with regular expression tokens and regular expression literals"() {
    when:
    handlers {
      prefix(":a:[a-c]{1,3}/:b") {
        path("::[a-z]{3}/:c?:4") {
          def binding = pathBinding
          render("$binding.tokens - $binding.allTokens - $binding.pastBinding")
        }
      }
    }

    then:
    getText("a/2/abc/4") == "[c:4] - [a:a, b:2, c:4] - "

    getText("c/2/abc") == "[:] - [a:c, b:2] - "

    get("a/2/123/4").statusCode == NOT_FOUND.code()
  }

  def "can use get handler"() {
    when:
    handlers {
      get {
        render("root")
      }

      get("a") {
        render("a")
      }
    }

    then:
    getText() == "root"
    getText("a") == "a"
    get("a/b/c").statusCode == NOT_FOUND.code()
  }

  def "can use post handler"() {
    when:
    handlers {
      post {
        render("root")
      }

      post("a") {
        render("a")
      }
    }

    then:
    postText() == "root"
    postText("a") == "a"
    post("a/b/c").statusCode == NOT_FOUND.code()
  }

  def "can use put handler"() {
    when:
    handlers {
      put {
        render("root")
      }

      put("a") {
        render("a")
      }
    }

    then:
    putText() == "root"
    putText("a") == "a"
    put("a/b/c").statusCode == NOT_FOUND.code()
  }

  def "can use patch handler"() {
    when:
    handlers {
      patch {
        render("root")
      }

      patch("a") {
        render("a")
      }
    }

    then:
    patchText() == "root"
    patchText("a") == "a"
    patch("a/b/c").statusCode == NOT_FOUND.code()
  }

  def "can use options handler"() {
    when:
    handlers {
      options {
        render("root")
      }

      options("a") {
        render("a")
      }
    }

    then:
    optionsText() == "root"
    optionsText("a") == "a"
    options("a/b/c").statusCode == NOT_FOUND.code()
  }

  def "can use delete handler"() {
    when:
    handlers {
      delete {
        render("root")
      }

      delete("a") {
        render("a")
      }
    }

    then:
    deleteText() == "root"
    deleteText("a") == "a"
    delete("a/b/c/d").statusCode == NOT_FOUND.code()
  }

  def "double-slashes in paths are not de-duped"() {
    when:
    handlers {
      get("a/b") {
        render "de-dup"
      }
      get("a//b") {
        render "no de-dup"
      }
    }

    then:
    getText("a//b") == "no de-dup"
  }

  def "double-slashes from empty path tokens are allowed"() {
    when:
    handlers {
      get(":a/:b?/:c?") {
        render "a=${pathTokens.a},b=${pathTokens.b},c=${pathTokens.c}"
      }
    }

    then:
    getText("1//3") == "a=1,b=,c=3"
  }

  def "leading slashes in the path are matched as-is after skipping the first"() {
    when:
    handlers {
      get("bar") {
        render "0"
      }
      get("/bar") {
        render "1"
      }
      get("//bar") {
        render "2"
      }
    }

    then:
    new URL("${applicationUnderTest.address}bar").text == "0"
    new URL("${applicationUnderTest.address}/bar").text == "1"
    new URL("${applicationUnderTest.address}//bar").text == "2"
  }

  def "trailing slashes in the path are matched as-is after skipping the first"() {
    when:
    handlers {
      get("bar") {
        render "0"
      }
      get("bar/") {
        render "1"
      }
      get("bar//") {
        render "2"
      }
    }

    then:
    getText("bar") == "0"
    getText("bar/") == "0"
    //    getText("bar//") == "1" // The httpurl spec under the covers will fix this bar// before it is ever passed to ratpack
  }
}
