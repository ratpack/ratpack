package org.ratpackframework.handler

import groovy.transform.CompileStatic
import org.ratpackframework.templating.TemplateRenderer
import org.vertx.java.core.Handler
import org.vertx.java.core.http.HttpServerRequest

@CompileStatic
class NotFoundHandler implements Handler<HttpServerRequest> {

  private final TemplateRenderer templateCompiler

  NotFoundHandler(TemplateRenderer templateCompiler) {
    this.templateCompiler = templateCompiler
  }

  @Override
  void handle(HttpServerRequest request) {
    request.response.statusCode = 404
    def model = [
        title: 'Page Not Found',
        message: 'Page Not Found',
        metadata: [
            'Request Method': request.method.toUpperCase(),
            'Request URL': request.uri,
        ]
    ] as Map<Object, Object>
    templateCompiler.renderError(model, new FallbackErrorHandlingTemplateRenderer(request, "404 handling"))
    true
  }
}
