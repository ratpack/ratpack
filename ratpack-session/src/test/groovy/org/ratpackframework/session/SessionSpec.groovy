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
      routing {
        get(":v") {
          response.send get(Session).id
        }
      }
    }

    then:
    urlGetText("a") == urlGetText("b")
  }

  def "can store session vars"() {
    when:
    app {
      routing {
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
    urlGetText("set/foo") == "foo"

    then:
    urlGetText() == "foo"
  }

  def "can invalidate session vars"() {
    when:
    app {
      routing {
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
    urlGetText("set/foo")

    then:
    urlGetText() == "foo"
    urlGetText("size") == "1"

    when:
    urlGetText("invalidate")

    then:
    urlGetText() == "null"
    urlGetText("size") == "1"
  }

  def "sessions are created on demand"() {
    when:
    app {
      routing {
        get {
          response.send get(MapSessionStore).size().toString()
        }
      }
    }

    then:
    urlGetText() == "0"

    when:
    app {
      routing {
        get {
          get(SessionStorage)
          response.send get(MapSessionStore).size().toString()
        }
      }
    }

    then:
    urlGetText() == "1"
  }
}
