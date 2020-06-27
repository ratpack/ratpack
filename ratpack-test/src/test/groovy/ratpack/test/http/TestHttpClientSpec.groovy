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

package ratpack.test.http

import ratpack.func.Action
import ratpack.test.internal.RatpackGroovyDslSpec

class TestHttpClientSpec extends RatpackGroovyDslSpec {

  def "tracks cookies over redirects"() {
    when:
    handlers {
      get {
        response.cookie("a", "a")
        redirect "2"
      }
      get("2") {
        render request.oneCookie("a")
      }
    }

    then:
    text == "a"
  }

  def "can use onredirect strategy"() {
    when:
    handlers {
      get {
        response.cookie("a", "a")
        redirect "2"
      }
      get("2") {
        render request.oneCookie("a") + ":" + request.headers.foo
      }
    }

    requestSpec {
      it.onRedirect { Action.from { it.headers.set("foo", "bar") } }
    }

    then:
    text == "a:bar"
  }

  def "can use onredirect strategy to stop redirect"() {
    when:
    handlers {
      get {
        response.cookie("a", "a")
        redirect "2"
      }
    }

    requestSpec {
      it.onRedirect { null }
    }

    then:
    get().statusCode == 302
    getCookies("")[0].name() == "a"
    getCookies("")[0].value() == "a"
  }

  def "client supports post-redirect-get"() {
    when:
    handlers {
      post("post") {
        redirect "get"
      }
      get("get") {
        render "ok"
      }
    }

    requestSpec {
      it.post()
    }

    then:
    post("post").statusCode == 200
    postText("post") == "ok"
  }

  def "doesn't modify verb for 303 and 307 supports post-redirect-get"() {
    when:
    handlers {
      post("303") {
        redirect 303, "ok"
      }
      post("307") {
        redirect 307, "ok"
      }
      post("ok") {
        render "ok"
      }
    }

    requestSpec {
      it.post()
    }

    then:
    postText("303") == "ok"
    postText("307") == "ok"
  }

}
