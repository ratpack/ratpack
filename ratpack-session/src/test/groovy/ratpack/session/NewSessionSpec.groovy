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

import ratpack.session.store.SessionStoreAdapter
import ratpack.test.internal.RatpackGroovyDslSpec

class NewSessionSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new NewSessionModule()
  }

  def "can use session"() {
    when:
    handlers {
      get { SessionAdapter session ->
        render session.set("foo", "bar")
          .next(session.require("foo"))
      }
    }

    then:
    text == "bar"
  }

  def "can store strings"() {
    when:
    handlers {
      get { SessionAdapter session ->
        render session.require("value")
      }
      get("set/:value") { SessionAdapter session ->
        render session.set("value", pathTokens.value).map { it.toString() }
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
      get { SessionAdapter session ->
        render session.require(Holder1).map { it.value }
      }
      get("set/:value") { SessionAdapter session ->
        def value = pathTokens.value
        session.set(new Holder1(value: value)).then {
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
      get { SessionAdapter session ->
          session
            .set(new Holder1(value: "1"))
            .next(session.set(new Holder2(value: "2")))
            .then { render "ok" }
      }
      get("get") { SessionAdapter session ->
        session.sync.then {
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
    when:
    handlers {
      get { SessionAdapter session ->
        session.sync.then {
          render it.get("value").orElse("null")
        }
      }
      get("set/:value") { SessionAdapter session ->
        session.sync.then {
          it.set("value", pathTokens.value)
          render pathTokens.value
        }
      }
      get("invalidate") { SessionAdapter session ->
        session.sync.then {
          it.terminate()
          render "ok"
        }
      }
      get("size") { SessionStoreAdapter storeAdapter ->
        render storeAdapter.size().toString()
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
    when:
    handlers {
      get { SessionStoreAdapter store ->
        render store.size().toString()
      }
      get("readOnly") { SessionAdapter session ->
        session.sync.then { render "ok" }
      }
      get("write") { SessionAdapter session ->
        session.sync.then { it.set("foo", "bar"); render "ok" }
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
      get("write") { SessionAdapter session ->
        session.sync.then { it.set("foo", "bar"); render "ok" }
      }
    }

    then:
    get("nowrite").headers.get("Set-Cookie") == null
    get("write").headers.get("Set-Cookie").contains('JSESSIONID')

    // null because the session cookieSessionId is already set
    !get("write").headers.get("Set-Cookie")?.contains('JSESSIONID')
  }
}
