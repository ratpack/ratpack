package org.ratpackframework.session

import org.ratpackframework.session.store.MapSessionStore
import org.ratpackframework.session.store.MapSessionsModule
import org.ratpackframework.session.store.SessionStorage
import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

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
          response.send get(MapSessionStore).size().toString()
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
          response.send get(MapSessionStore).size().toString()
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
          response.send get(MapSessionStore).size().toString()
        }
      }
    }

    then:
    getText() == "1"
  }
}
