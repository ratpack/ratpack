/*
 * Copyright 2015 the original author or authors.
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

package ratpack.session.clientside

import io.netty.handler.codec.http.Cookie
import ratpack.session.store.SessionStorage
import ratpack.test.internal.RatpackGroovyDslSpec

class ClientSideSessionCookiesSpec extends RatpackGroovyDslSpec {
  private String[] getSessionCookies(String path) {
    List<Cookie> pathCookies = getCookies(path)
    if (pathCookies == null) {
      return []
    }
    pathCookies.stream().findAll { it.name.startsWith("ratpack_session")?.value }.toArray()
  }

  private def getSessionAttrs(SessionService sessionService, String path) {
    List<Cookie> cookies = getCookies(path)
    def attrs = [:]
    cookies
      .stream()
      .filter{ c ->
        c.name().startsWith("ratpack_session") == true
      }
      .map { c ->
        def m = sessionService.deserializeSession(c)
        if (m) {
          attrs.putAll(m)
        }
        return c
      }
      .collect()
    return attrs
  }

  def "cookies assigned to path are send for this path only"() {
    given:
    bindings {
      add ClientSideSessionsModule, {
        it.with {
          path = "/bar"
        }
      }
    }

    handlers {
      get("foo") { SessionStorage sessionStorage ->
        sessionStorage.get("value", String).then({
          render it.orElse("null")
        })
      }
      get("bar") { SessionStorage sessionStorage ->
        sessionStorage.get("value", String).then({
          render it.orElse("null")
        })
      }
      get("") { SessionStorage sessionStorage ->
        sessionStorage.get("value", String).then({
          render it.orElse("null")
        })
      }
      get("val/:value") { SessionStorage sessionStorage ->
        sessionStorage.set("value", pathTokens.value).then({
          sessionStorage.get("value", String).then({
            render it.orElse("null")
          })
        })
      }
    }

    when:
    get("foo")
    String[] setCookiesRoot = getSessionCookies("/")
    String[] setCookiesBar = getSessionCookies("/bar")
    String[] setCookiesVal = getSessionCookies("/val")

    then:
    setCookiesRoot.length == 0
    setCookiesBar.length == 0
    setCookiesVal.length == 0

    when:
    get("val/foo")
    setCookiesRoot = getSessionCookies("/")
    setCookiesBar = getSessionCookies("/bar")
    setCookiesVal = getSessionCookies("/val")

    then:
    setCookiesRoot.length == 0
    setCookiesBar.length == 1
    setCookiesVal.length == 0
  }

  def "cookie is send in request for the given path only"() {
    given:
    bindings {
      add ClientSideSessionsModule, {
        it.with {
          path = "/foo"
        }
      }
    }
    handlers {
      get("/foo/:value") { SessionStorage sessionStorage ->
        if (pathTokens.value == "check") {
          sessionStorage.get("value", String).then({
            render it.orElse("null")
          })
        } else {
          sessionStorage.set("value", pathTokens.value).then({
            sessionStorage.get("value", String).then({
              render it.orElse("null")
            })
          })
        }
      }
      get("/bar") { SessionStorage sessionStorage ->
        sessionStorage.get("value", String).then({
          render it.orElse("null")
        })
      }
    }

    when:
    get("/foo/val1")
    get("/bar")

    then:
    response.body.text == "null"

    when:
    get("/foo/check")

    then:
    response.body.text == "val1"
  }

  def "removed session attribute is no longer accessible"() {
    given:
    bindings {
      add ClientSideSessionsModule, {
        it.with {
          secretKey = "aaaaaaaaaaaaaaaa"
        }
      }
    }
    def clientSessionService = null
    handlers {
      if (!clientSessionService) {
        clientSessionService = registry.get(SessionService)
      }
      get("foo/:value") { SessionStorage sessionStorage ->
        sessionStorage.set("foo", pathTokens.value).then({
          sessionStorage.get("foo", String).then({
            render it.orElse("null")
          })
        })
      }
      get("clear") { SessionStorage sessionStorage ->
        sessionStorage.remove("foo").then({
          render "null"
        })
      }
    }

    when:
    get("foo/val1")
    String[] setCookies = getSessionCookies("/")
    clientSessionService
    def attrs = getSessionAttrs(clientSessionService, "/")

    then:
    setCookies.length == 1
    attrs.size() == 1
    attrs.foo == "val1"

    when:
    get("clear")
    setCookies = getSessionCookies("/")
    attrs = getSessionAttrs(clientSessionService, "/")

    then:
    setCookies.length == 0
    attrs.size() == 0
  }

  def "removed session attribute does not clear the other attributes"() {
    bindings {
      add ClientSideSessionsModule, {
        it.with {
          secretKey = "aaaaaaaaaaaaaaaa"
        }
      }
    }
    def clientSessionService = null
    handlers {
      if (!clientSessionService) {
        clientSessionService = registry.get(SessionService)
      }
      get("s/:attr/:value") { SessionStorage sessionStorage ->
        String attr = pathTokens.attr
        String value = pathTokens.value
        if (attr && value) {
          sessionStorage.set(attr, value).then({
            render "ATTR: ${attr} VALUE: ${sessionStorage[attr]?.toString()}"
          })
        } else {
          clientError(404)
        }
      }
      get("clear/:attr") { SessionStorage sessionStorage ->
        String attr = pathTokens.attr
        if (attr) {
          sessionStorage.remove(attr).then({
            sessionStorage.get(attr, String).onError({th ->
              render "null"
            }).then({
              render it.orElse("null")
            })
          })
        } else {
          clientError(404)
        }
      }
    }

    when:
    get("s/foo/bar")
    get("s/baz/quux")
    String[] setCookies = getSessionCookies("/")
    def attrs = getSessionAttrs(clientSessionService, "/")

    then:
    setCookies.length == 1
    attrs.size() == 2
    attrs["foo"] == "bar"
    attrs["baz"] == "quux"

    when:
    get("clear/foo")
    setCookies = getSessionAttrs(clientSessionService, "/")
    attrs = getSessionAttrs(clientSessionService, "/")

    then:
    setCookies.length == 1
    attrs.size() == 1
    !attrs["foo"]
    attrs["baz"] == "quux"
  }

  def "large session partitioned into session cookies"() {
    bindings {
      add ClientSideSessionsModule, {
        it.with {
          secretKey = "aaaaaaaaaaaaaaaa"
        }
      }
    }

    handlers {
      get("set") { SessionStorage sessionStorage ->
        String value = ""
        for (int i = 0; i < 1024; i++) {
          value += "ab"
        }
        sessionStorage.set("foo", value).then({
          sessionStorage.get("foo", String).then({
            render it.orElse("null")
          })
        })
      }
      get("setsmall") { SessionStorage sessionStorage ->
        sessionStorage.set("foo", "val1").then({
          sessionStorage.get("foo", String).then({
            render it.orElse("null")
          })
        })
      }
      get("clear") { SessionStorage sessionStorage ->
        sessionStorage.remove("foo").then({
          render ""
        })
      }
    }

    when:
    get("set")

    then:
    getCookies("/").size() == 2

    when:
    get("setsmall")

    then:
    getCookies("/").size() == 1

    when:
    get("clear")

    then:
    getCookies("/").size == 0

    when:
    get("set")

    then:
    getCookies("/").size() == 2

    when:
    get("clear")

    then:
    getCookies("/").size() == 0
  }
}
