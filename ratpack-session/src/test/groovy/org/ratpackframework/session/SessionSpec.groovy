package org.ratpackframework.session

import com.google.inject.Injector
import org.ratpackframework.guice.GuiceBackedHandlerFactory
import org.ratpackframework.guice.ModuleRegistry
import org.ratpackframework.guice.internal.DefaultGuiceBackedHandlerFactory
import org.ratpackframework.routing.Exchange
import org.ratpackframework.routing.Handler
import org.ratpackframework.session.internal.SessionBindingHandler
import org.ratpackframework.session.store.MapSessionStore
import org.ratpackframework.session.store.MapSessionsModule
import org.ratpackframework.session.store.SessionStorage
import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

class SessionSpec extends RatpackGroovyDslSpec {

  @Override
  protected GuiceBackedHandlerFactory createAppFactory() {
    new DefaultGuiceBackedHandlerFactory() {
      @Override
      protected void registerDefaultModules(ModuleRegistry moduleRegistry) {
        moduleRegistry.register(new SessionModule())
        moduleRegistry.register(new MapSessionsModule(10, 5))
        super.registerDefaultModules(moduleRegistry)
      }
    }
  }

  @Override
  Handler decorateHandler(Handler handler) {
    super.decorateHandler(new SessionBindingHandler(new Handler() {
      @Override
      void handle(Exchange exchange) {
        // TODO a proper handler impl for this

        def injector = exchange.context.get(Injector)
        def mapSessionStore = injector.getInstance(MapSessionStore)

        // TODO we are creating a session map even if we dont' use use it
        // We should avoid doing so unless some really asks for the session storage
        def sessionStorage = mapSessionStore.get(exchange.context.maybeGet(Session).id)
        exchange.nextWithContext(sessionStorage, handler)
      }
    }))
  }

  def "can use session"() {
    when:
    app {
      routing {
        get(":v") {
          response.send context.maybeGet(Session).getId()
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
          def store = context.get(SessionStorage)
          response.send store.value.toString()
        }
        get("set/:value") {
          def store = context.get(SessionStorage)
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
          def store = context.get(SessionStorage)
          response.send store.value ?: "null"
        }
        get("set/:value") {
          def store = context.get(SessionStorage)
          store.value = pathTokens.value
          response.send store.value ?: "null"
        }
        get("invalidate") {
          context.maybeGet(Session).terminate()
          response.send()
        }
        get("size") {
          response.send context.maybeGet(MapSessionStore).size().toString()
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

}
