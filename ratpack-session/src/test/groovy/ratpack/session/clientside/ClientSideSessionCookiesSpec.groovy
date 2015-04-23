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
  def setup() {
    modules << new ClientSideSessionsModule()
  }

  private String[] getSessionCookies(String path) {
    List<Cookie> pathCookies = getCookies(path)
    if (pathCookies == null) {
      return []
    }
    pathCookies.stream().findAll { it.name.startsWith("ratpack_session")?.value }.toArray()
  }

  def "cookies assigned to path are send for this path only"() {
    given:
    modules.clear()
    bindings {
      add ClientSideSessionsModule, {
        it.with {
          path = "/bar"
        }
      }
    }

    handlers {
      get("foo") { SessionStorage sessionStorage ->
        render sessionStorage.value?.toString()
      }
      get("bar") { SessionStorage sessionStorage ->
        render sessionStorage.value?.toString()
      }
      get("") { SessionStorage sessionStorage ->
        render sessionStorage.value?.toString()
      }
      get("val/:value") { SessionStorage sessionStorage ->
        sessionStorage.value = pathTokens.value
        render sessionStorage.value.toString()
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
    modules.clear()
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
          render sessionStorage.value?.toString()
        } else {
          sessionStorage.value = pathTokens.value
          render sessionStorage.value.toString()
        }
      }
      get("/bar") { SessionStorage sessionStorage ->
        render sessionStorage.value?.toString()
      }
    }

    when:
    get("/foo/val1")
    get("/bar")

    then:
    response.body.text == ""

    when:
    get("/foo/check")

    then:
    response.body.text == "val1"
  }
}
