package org.ratpackframework.handler

import org.ratpackframework.templating.TemplateRenderer
import org.vertx.java.core.http.HttpServerRequest

class NotFoundHandler implements MaybeHandler<HttpServerRequest> {

  private final TemplateRenderer renderer

  NotFoundHandler(TemplateRenderer renderer) {
    this.renderer = renderer
  }

  @Override
  boolean maybeHandle(HttpServerRequest request) {
    request.response.statusCode = 404
    def string = renderer.renderError(
        title: 'Page Not Found',
        message: 'Page Not Found',
        metadata: [
            'Request Method': request.method.toUpperCase(),
            'Request URL': request.uri,
        ]
    )
    request.response.end(string)
    true
  }
}
