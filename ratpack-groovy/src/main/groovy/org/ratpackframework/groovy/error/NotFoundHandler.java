package org.ratpackframework.groovy.error;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.error.FallbackErrorHandler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;
import org.ratpackframework.templating.TemplateRenderer;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class NotFoundHandler implements Handler<Routed<HttpExchange>> {

  private final TemplateRenderer templateCompiler;

  @Inject
  public NotFoundHandler(TemplateRenderer templateRenderer) {
    this.templateCompiler = templateRenderer;
  }

  @Override
  public void handle(Routed<HttpExchange> routedHttpExchange) {
    HttpExchange exchange = routedHttpExchange.get();
    HttpRequest request = exchange.getRequest();
    exchange.getResponse().setStatus(HttpResponseStatus.NOT_FOUND);
    Map<String, Object> model = new HashMap<>(3);
    model.put("title", "Page Not Found");
    model.put("message", "Page Not Found");
    Map<String, Object> metadata = new HashMap<>(2);
    metadata.put("Request Method", request.getMethod().getName());
    metadata.put("Request URL", request.getUri());
    model.put("metadata", metadata);
    templateCompiler.renderError(model, new FallbackErrorHandler(exchange, "404 handling"));
  }
}
