package org.ratpackframework.groovy.templating;

import org.ratpackframework.groovy.templating.internal.TemplateRendererBindingHandler;
import org.ratpackframework.groovy.templating.internal.TemplateRenderingErrorHandler;
import org.ratpackframework.routing.Handler;

public abstract class TemplatingHandlers {

  public static Handler templates(String templatesDir, Handler handler) {
    return new TemplateRendererBindingHandler(templatesDir, new TemplateRenderingErrorHandler(handler));
  }
}
