package org.ratpackframework.guice

import com.google.inject.AbstractModule
import org.ratpackframework.test.groovy.RatpackGroovyDslSpec
import org.ratpackframework.routing.Exchange
import org.ratpackframework.routing.Handler

import javax.inject.Inject

import static org.ratpackframework.guice.Injection.handler

class InjectedHandlersSpec extends RatpackGroovyDslSpec {

  static class Injectable {
    String name
  }

  static class InjectedHandler implements Handler {

    Injectable injectable

    @Inject
    InjectedHandler(Injectable injectable) {
      this.injectable = injectable
    }

    @Override
    void handle(Exchange exchange) {
      exchange.response.send(injectable.name)
    }
  }

  def "can use injected handlers"() {
    given:
    def nameValue = "foo"

    when:
    app {
      modules {
        register(new AbstractModule() {
          @Override
          protected void configure() {
            bind(Injectable).toInstance(new Injectable(name: nameValue))
          }
        })
      }
      routing {
        route handler(InjectedHandler)
      }
    }

    then:
    urlGetText("") == nameValue
  }

}
