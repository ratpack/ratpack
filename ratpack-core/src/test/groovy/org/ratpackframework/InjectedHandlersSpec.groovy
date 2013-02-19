package org.ratpackframework

import com.google.inject.AbstractModule
import org.ratpackframework.app.Request
import org.ratpackframework.app.Response
import org.ratpackframework.app.internal.DefaultRequest
import org.ratpackframework.handler.Handler
import org.ratpackframework.templating.NullTemplateRenderer
import org.ratpackframework.templating.TemplateRenderer
import org.ratpackframework.test.DefaultRatpackSpec

import javax.inject.Inject

class InjectedHandlersSpec extends DefaultRatpackSpec {

  @javax.inject.Singleton
  static class InjectedHandler implements Handler<Response> {
    TemplateRenderer templateRenderer

    @Inject InjectedHandler(TemplateRenderer templateRenderer) {
      this.templateRenderer = templateRenderer
    }

    @Override
    void handle(Response response) {
      response.text(templateRenderer.class.name)
    }
  }

  def "can use injected handlers"() {
    given:
    routing {
      get("/", InjectedHandler)
    }

    when:
    startApp()

    then:
    urlGetText("") == NullTemplateRenderer.name
  }

  static class InjectedRequestHandler implements Handler<Response> {
    @Inject Request request

    @Override
    void handle(Response response) {
      response.text(request.class.name)
    }
  }

  def "can inject request"() {
    given:
    routing {
      get("/", InjectedRequestHandler)
    }

    when:
    startApp()

    then:
    urlGetText("") == DefaultRequest.name
  }

  static class RequestService {
    @Inject Request request
  }

  def "can obtain request scoped services in closures"() {
    given:
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(RequestService)
      }
    }

    and:
    routing {
      get("/") {
        text service(RequestService).request.class.name
      }
    }

    when:
    startApp()

    then:
    urlGetText() == DefaultRequest.name
  }
}
