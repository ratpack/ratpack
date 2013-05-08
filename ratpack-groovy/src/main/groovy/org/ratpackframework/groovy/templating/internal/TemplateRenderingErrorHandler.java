package org.ratpackframework.groovy.templating.internal;

import org.ratpackframework.groovy.templating.TemplateRenderer;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

public class TemplateRenderingErrorHandler implements Handler {

  private final Handler delegate;

  public TemplateRenderingErrorHandler(Handler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    try {
      exchange.next(delegate);
    } catch (Exception exception) {
      TemplateRenderer renderer = exchange.getContext().require(TemplateRenderer.class);
      renderer.error(ExceptionToTemplateModel.transform(exchange.getRequest(), exception));
    }
  }
}
