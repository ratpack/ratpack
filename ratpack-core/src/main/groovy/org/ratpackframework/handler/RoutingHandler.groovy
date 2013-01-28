package org.ratpackframework.handler

import org.ratpackframework.responder.FinalizedResponse
import org.ratpackframework.routing.Router
import org.ratpackframework.routing.internal.RoutedRequest
import org.vertx.java.core.Handler
import org.vertx.java.core.http.HttpServerRequest

class RoutingHandler implements Handler<HttpServerRequest> {

  private final Router router

  private ErrorHandler errorHandler

  private Handler<HttpServerRequest> notFoundHandler

  RoutingHandler(Router router, ErrorHandler errorHandler, Handler<HttpServerRequest> notFoundHandler) {
    this.router = router
    this.errorHandler = errorHandler
    this.notFoundHandler = notFoundHandler
  }

  @Override
  void handle(HttpServerRequest request) {
    RoutedRequest routedRequest = new RoutedRequest(request, errorHandler, notFoundHandler, errorHandler.asyncHandler(request, new Handler<FinalizedResponse>() {
      @Override
      void handle(FinalizedResponse response) {
        def realResponse = request.response

        realResponse.statusCode = response.status

        response.headers.each { k, v ->
          (v instanceof Iterable ? v.toList() : [v]).each {
            realResponse.putHeader(k.toString(), it.toString())
          }
        }

        realResponse.end(response.buffer)
      }
    }))

    router.handle(routedRequest)
  }

}
