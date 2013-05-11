package org.ratpackframework.groovy.templating.internal;

import org.ratpackframework.file.FileSystemContext;
import org.ratpackframework.groovy.templating.TemplateRenderer;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

import java.io.File;

public class TemplateRendererBindingHandler implements Handler {

  private final String templateDir;
  private final Handler delegate;

  public TemplateRendererBindingHandler(String templateDir, Handler delegate) {
    this.templateDir = templateDir;
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    GroovyTemplateRenderingEngine engine = exchange.get(GroovyTemplateRenderingEngine.class);

    File templateDirFile = exchange.get(FileSystemContext.class).file(templateDir);

    TemplateRenderer renderer = new DefaultTemplateRenderer(templateDirFile, exchange, engine);
    exchange.nextWithContext(renderer, delegate);
  }
}
