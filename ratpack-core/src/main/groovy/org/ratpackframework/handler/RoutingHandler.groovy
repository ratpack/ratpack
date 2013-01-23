package org.ratpackframework.handler

import org.ratpackframework.responder.FinalizedResponse
import org.ratpackframework.responder.Responder
import org.ratpackframework.routing.Router
import org.ratpackframework.templating.TemplateRenderer
import org.vertx.java.core.buffer.Buffer
import org.vertx.java.core.http.HttpServerRequest

class RoutingHandler implements MaybeHandler<HttpServerRequest> {

  private final Router router

  private final TemplateRenderer renderer

  RoutingHandler(Router router, TemplateRenderer renderer) {
    this.router = router
    this.renderer = renderer
  }

  @Override
  boolean maybeHandle(HttpServerRequest request) {
    Responder responder = router.route(request)
    if (!responder) {
      return false
    }

    FinalizedResponse response = responder.respond()
    def realResponse = request.response

    realResponse.statusCode = response.status

    response.headers.each { k, v ->
      (v instanceof Iterable ? v.toList() : [v]).each {
        realResponse.putHeader(k.toString(), it.toString())
      }
    }

    realResponse.end(new Buffer(response.bytes))
    true
  }

}
