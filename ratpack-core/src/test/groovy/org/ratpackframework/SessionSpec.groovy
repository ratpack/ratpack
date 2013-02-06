package org.ratpackframework

import org.ratpackframework.session.store.MapSessionStore
import org.ratpackframework.session.store.MapSessionsModule

class SessionSpec extends RatpackSpec {

  def "can use session"() {
    given:
    ratpackFile << """
      get("/:v") {
        renderText it.session.getId()
      }
    """

    when:
    startApp()

    then:
    urlGetText("a") == urlGetText("b")
  }

  def "can store session vars"() {
    given:
    config.modules << new MapSessionsModule(10, 10)

    ratpackFile << """
      get("/") {
        def store = service(${MapSessionStore.name}).get(it)
        renderText store.value
      }
      get("/set/:value") {
        def store = service(${MapSessionStore.name}).get(it)
        store.value = it.urlParams.value
        renderText store.value
      }
    """

    when:
    startApp()

    and:
    urlGetText("set/foo") == "foo"

    then:
    urlGetText() == "foo"
  }

  def "can invalidate session vars"() {
    given:
    config.modules << new MapSessionsModule(10, 10)

    ratpackFile << """
      get("/") {
        def store = service(${MapSessionStore.name}).get(it)
        renderText store.value
      }
      get("/set/:value") {
        def store = service(${MapSessionStore.name}).get(it)
        store.value = it.urlParams.value
        renderText store.value
      }
      get("/invalidate") {
        it.session.terminate()
        end()
      }
      get("/size") {
        renderText service(${MapSessionStore.name}).size()
      }
    """

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
