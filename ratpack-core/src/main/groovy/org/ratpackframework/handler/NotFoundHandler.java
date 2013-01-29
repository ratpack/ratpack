package org.ratpackframework.handler;

import org.ratpackframework.templating.TemplateRenderer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.HashMap;
import java.util.Map;

public class NotFoundHandler implements Handler<HttpServerRequest> {

  private final TemplateRenderer templateCompiler;

  public NotFoundHandler(TemplateRenderer templateCompiler) {
    this.templateCompiler = templateCompiler;
  }

  @Override
  public void handle(HttpServerRequest request) {
    request.resume();
    request.response.statusCode = 404;
    Map<String, Object> model = new HashMap<String, Object>(3);
    model.put("title", "Page Not Found");
    model.put("message", "Page Not Found");
    Map<String, Object> metadata = new HashMap<String, Object>(2);
    metadata.put("Request Method", request.method.toUpperCase());
    metadata.put("Request URL", request.uri);
    model.put("metadata", metadata);
    templateCompiler.renderError(model, new FallbackErrorHandlingTemplateRenderer(request, "404 handling"));
  }
}
