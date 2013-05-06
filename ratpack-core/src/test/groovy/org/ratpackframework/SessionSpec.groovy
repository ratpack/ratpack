package org.ratpackframework

import org.ratpackframework.http.Request
import org.ratpackframework.session.store.MapSessionStore
import org.ratpackframework.session.store.MapSessionsModule
import org.ratpackframework.test.DefaultRatpackSpec

class SessionSpec extends DefaultRatpackSpec {

  def "can use session"() {
    given:
    routing {
      get("/:v") {
        text it.session.getId()
      }
    }

    when:
    startApp()

    then:
    urlGetText("a") == urlGetText("b")
  }

  def "can store session vars"() {
    given:
    modules {
      register(new MapSessionsModule(10, 10))
    }

    routing {
      get("/") {
        def store = objects.get(MapSessionStore).get(it)
        text store.value
      }
      get("/set/:value") {
        def store = objects.get(MapSessionStore).get(it)
        store.value = it.pathParams.value
        text store.value
      }
    }

    when:
    startApp()

    and:
    urlGetText("set/foo") == "foo"

    then:
    urlGetText() == "foo"
  }

  def "can invalidate session vars"() {
    given:
    modules {
      register(new MapSessionsModule(10, 10))
    }

    routing {
      get("/") {
        def store = objects.get(MapSessionStore).get(it)
        text store.value
      }
      get("/set/:value") {
        def store = objects.get(MapSessionStore).get(it)
        store.value = it.pathParams.value
        text store.value
      }
      get("/invalidate") { Request request ->
        request.session.terminate()
        end()
      }
      get("/size") {
        text objects.get(MapSessionStore).size()
      }
    }

    when:
    startApp()

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

}
