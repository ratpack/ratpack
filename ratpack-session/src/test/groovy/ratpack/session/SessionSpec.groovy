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

package ratpack.session

import ratpack.test.internal.RatpackGroovyDslSpec

class SessionSpec extends RatpackGroovyDslSpec {

  boolean supportsSize = true

  def setup() {
    modules << new SessionModule()
  }

  def "can use session"() {
    when:
    handlers {
      get { Session session ->
        session.data.then {
          it.set("foo", "bar")
          render it.require("foo")
        }
      }
    }

    then:
    text == "bar"
  }

  def "can store strings"() {
    when:
    handlers {
      get { Session session ->
        render session.data.map { it.require("value") }
      }
      get("set/:value") { Session session ->
        session.data.then {
          it.set("value", pathTokens.value)
          render pathTokens.value
        }
      }
    }

    and:
    getText("set/foo") == "false"

    then:
    getText() == "foo"
  }

  static class Holder1 implements Serializable {
    String value
  }

  static class Holder2 implements Serializable {
    String value
  }

  def "can store objects"() {
    when:
    handlers {
      get { Session session ->
        render session.data.map { it.require(Holder1).value }
      }
      get("set/:value") { Session session ->
        def value = pathTokens.value
        session.data.then {
          it.set(new Holder1(value: value))
          render value
        }
      }
    }

    and:
    getText("set/foo") == "foo"

    then:
    getText() == "foo"
  }

  def "objects are differentiated"() {
    when:
    handlers {
      get { Session session ->
        session.data.then {
          it.set(new Holder1(value: "1"))
          it.set(new Holder2(value: "2"))
          render "ok"
        }
      }
      get("get") { Session session ->
        session.data.then {
          render it.require(Holder1).value + it.require(Holder2).value
        }
      }
    }

    and:
    get() // put data

    then:
    getText("get") == "12"
  }

  def "can invalidate session vars"() {
    if (!supportsSize) {
      return
    }

    when:
    handlers {
      get { Session session ->
        session.data.then {
          render it.get("value").orElse("null")
        }
      }
      get("set/:value") { Session session ->
        session.data.then {
          it.set("value", pathTokens.value)
          render pathTokens.value
        }
      }
      get("invalidate") { Session session ->
        session.data.then {
          it.terminate().then { render "ok" }
        }
      }
      get("size") { SessionStore storeAdapter ->
        render storeAdapter.size().map { it.toString() }
      }
    }

    and:
    getText("set/foo")

    then:
    getText() == "foo"
    getText("size") == "1"
    getText("invalidate") == "ok"
    getText() == "null"
    getText("size") == "0"
  }

  def "sessions are created on demand"() {
    if (!supportsSize) {
      return
    }

    when:
    handlers {
      get { SessionStore store ->
        render store.size().map { it.toString() }
      }
      get("readOnly") { Session session ->
        session.data.then { render "ok" }
      }
      get("write") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok" }
      }
    }

    then:
    getText() == "0"
    getText("readOnly") == "ok"
    getText("write") == "ok"
    getText() == "1"
    getText("write") == "ok"
    getText() == "1"
  }

  def "session cookies are only set when needed"() {
    when:
    handlers {
      get("nowrite") {
        response.send("foo")
      }
      get("write") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok" }
      }
    }

    then:
    get("nowrite").headers.get("Set-Cookie") == null
    get("write").headers.get("Set-Cookie").contains('JSESSIONID')

    // null because the session cookieSessionId is already set
    !get("write").headers.get("Set-Cookie")?.contains('JSESSIONID')
  }

  def "session cookies are all HTTPOnly"() {
    when:
    handlers {
      get("foo") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok" }
      }
    }

    then:
    def values = get("foo").headers.getAll("Set-Cookie")
    values.findAll { it.contains("JSESSIONID") && it.contains("HTTPOnly") }.size() == 1
  }

  def "session cookies are not HTTPOnly"() {
    given:
    modules.clear()
    bindings {
      module SessionModule, {
        it.httpOnly = false
      }

    }
    handlers {
      get("foo") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok" }
      }
    }

    when:
    def values = get("foo").headers.getAll("Set-Cookie")

    then:
    values.findAll { it.contains("JSESSIONID") && !it.contains("HTTPOnly") }.size() == 1
  }

  def "session cookies are all Secure, can be transmitted via secure protocol"() {
    given:
    modules.clear()
    bindings {
      module SessionModule, {
        it.secure = true
      }
    }
    handlers {
      get("foo") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok" }
      }
    }

    when:
    def values = get("foo").headers.getAll("Set-Cookie")

    then:
    values.findAll { it.contains("JSESSIONID") && it.contains("Secure") }.size() == 1
  }
}
