package org.ratpackframework

import com.google.inject.AbstractModule
import com.google.inject.servlet.RequestScoped
import org.ratpackframework.error.ErrorHandler
import org.ratpackframework.internal.DefaultRequest

import javax.inject.Inject

class InjectedHandlersSpec extends RatpackSpec {

  @Singleton
  static class InjectedHandler implements Handler<Response> {
    ErrorHandler errorHandler

    @Inject InjectedHandler(ErrorHandler errorHandler) {
      this.errorHandler = errorHandler
    }

    @Override
    void handle(Response response) {
      response.text(errorHandler.class.name)
    }
  }

  def "can use injected handlers"() {
    given:
    ratpackFile << """
      get("/", ${InjectedHandler.name})
    """

    when:
    startApp()

    then:
    urlGetText("") == ErrorHandler.name
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
    ratpackFile << """
      get("/", ${InjectedRequestHandler.name})
    """

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
    config.modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(RequestService)
      }
    }

    and:
    ratpackFile << """
      get("/") {
        text service(${RequestService.name}).request.class.name
      }
    """

    when:
    startApp()

    then:
    urlGetText() == DefaultRequest.name
  }
}
