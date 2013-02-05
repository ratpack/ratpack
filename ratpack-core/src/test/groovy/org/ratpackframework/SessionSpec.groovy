package org.ratpackframework

import org.ratpackframework.service.ServiceRegistry
import org.ratpackframework.service.ServiceRegistryBuilder
import org.ratpackframework.session.store.ConcurrentMapSessionStore

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
    def store = new ConcurrentMapSessionStore(10, 10)
    config.sessionListener store.asSessionListener()
    config.services new ServiceRegistryBuilder().add("sessionStore", store).build()

    ratpackFile << """
      get("/") {
        def store = services.sessionStore.get(it)
        renderText store.value
      }
      get("/set/:value") {
        def store = services.sessionStore.get(it)
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

}
