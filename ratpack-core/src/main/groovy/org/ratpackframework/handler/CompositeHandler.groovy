package org.ratpackframework.handler


import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.StackTraceUtils
import org.vertx.java.core.Handler
import org.vertx.java.core.http.HttpServerRequest

@CompileStatic
class CompositeHandler implements Handler<HttpServerRequest> {

  private final List<MaybeHandler<HttpServerRequest>> handlers;

  private TemplateRenderer renderer

  CompositeHandler(TemplateRenderer renderer, List<MaybeHandler<HttpServerRequest>> handlers) {
    this.renderer = renderer
    this.handlers = new ArrayList(handlers);
  }

  @Override
  void handle(HttpServerRequest request) {
    try {
      for (MaybeHandler<HttpServerRequest> handler : handlers) {
        if (handler.maybeHandle(request)) {
          break
        }
      }
    } catch (Throwable error) {
      StackTraceUtils.deepSanitize(error)
      error.printStackTrace()
      def out = renderer.renderException(error, request)
      request.response.statusCode = 500
      request.response.end(out)
    }
  }

}
