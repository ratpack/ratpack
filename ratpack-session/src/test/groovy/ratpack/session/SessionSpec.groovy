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

package ratpack.session

import ratpack.session.store.MapSessionsModule
import ratpack.session.store.SessionStorage
import ratpack.session.store.SessionStore
import ratpack.test.internal.RatpackGroovyDslSpec

class SessionSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new SessionModule()
    modules << new MapSessionsModule(10, 5)
  }

  def "can use session"() {
    when:
    app {
      handlers {
        get(":v") {
          response.send get(Session).id
        }
      }
    }

    then:
    getText("a") == getText("b")
  }

  def "can store session vars"() {
    when:
    app {
      handlers {
        get("") {
          def store = get(SessionStorage)
          response.send store.value.toString()
        }
        get("set/:value") {
          def store = get(SessionStorage)
          store.value = pathTokens.value
          response.send store.value.toString()
        }
      }
    }

    and:
    getText("set/foo") == "foo"

    then:
    getText() == "foo"
  }

  def "can invalidate session vars"() {
    when:
    app {
      handlers {
        get("") {
          def store = get(SessionStorage)
          response.send store.value ?: "null"
        }
        get("set/:value") {
          def store = get(SessionStorage)
          store.value = pathTokens.value
          response.send store.value ?: "null"
        }
        get("invalidate") {
          get(Session).terminate()
          response.send()
        }
        get("size") {
          response.send get(SessionStore).size().toString()
        }
      }
    }

    and:
    getText("set/foo")

    then:
    getText() == "foo"
    getText("size") == "1"

    when:
    getText("invalidate")

    then:
    getText() == "null"
    getText("size") == "1"
  }

  def "sessions are created on demand"() {
    when:
    app {
      handlers {
        get {
          response.send get(SessionStore).size().toString()
        }
      }
    }

    then:
    getText() == "0"

    when:
    app {
      handlers {
        get {
          get(SessionStorage)
          response.send get(SessionStore).size().toString()
        }
      }
    }

    then:
    getText() == "1"
  }

  def "session cookies are only set when needed"() {
    when:
    app {
      handlers {
        get("foo") {
          response.send("foo")
        }
        get("bar") {
          get(SessionStorage) // just retrieve
          response.send("bar")
        }
      }
    }

    then:
    get("foo").cookies().isEmpty()
    get("bar").cookies().JSESSIONID != null

    // null because the session id is already set
    get("bar").cookies().JSESSIONID == null
  }
}
