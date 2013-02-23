package org.ratpackframework

import com.google.inject.AbstractModule
import org.ratpackframework.app.Endpoint
import org.ratpackframework.app.Request
import org.ratpackframework.app.Response
import org.ratpackframework.app.internal.DefaultRequest
import org.ratpackframework.templating.NullTemplateRenderer
import org.ratpackframework.templating.TemplateRenderer
import org.ratpackframework.test.DefaultRatpackSpec

import javax.inject.Inject

class InjectedEndpointsSpec extends DefaultRatpackSpec {

  @javax.inject.Singleton
  static class InjectedEndpoint implements Endpoint {
    TemplateRenderer templateRenderer

    @Inject
    InjectedHandler(TemplateRenderer templateRenderer) {
      this.templateRenderer = templateRenderer
    }

    @Override
    void respond(Request request, Response response) {
      response.text(templateRenderer.class.name)
    }
  }

  def "can use injected handlers"() {
    given:
    routing {
      get("/", inject(InjectedEndpoint))
    }

    when:
    startApp()

    then:
    urlGetText("") == NullTemplateRenderer.name
  }

  static class InjectedRequestEndpoint implements Endpoint {
    @Inject Request injectedRequest

    @Override
    void respond(Request request, Response response) {
      assert request.is(injectedRequest)
      response.text(injectedRequest.getClass().name)
    }
  }

  def "can inject request"() {
    given:
    routing {
      get("/", inject(InjectedRequestEndpoint))
    }

    when:
    startApp()

    then:
    urlGetText("") == DefaultRequest.name
  }

  static class RequestService {
    @Inject Request injectedRequest
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
        text service(RequestService).injectedRequest.class.name
      }
    }

    when:
    startApp()

    then:
    urlGetText() == DefaultRequest.name
  }
}
