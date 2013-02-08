package org.ratpackframework

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
}
