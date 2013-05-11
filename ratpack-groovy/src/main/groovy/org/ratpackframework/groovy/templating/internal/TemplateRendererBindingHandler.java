package org.ratpackframework.groovy.templating.internal;

import com.google.inject.Injector;
import org.ratpackframework.context.Context;
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
    Context context = exchange.getContext();
    Injector injector = context.get(Injector.class);
    GroovyTemplateRenderingEngine engine = injector.getInstance(GroovyTemplateRenderingEngine.class);

    File templateDirFile = context.require(FileSystemContext.class).file(templateDir);

    TemplateRenderer renderer = new DefaultTemplateRenderer(templateDirFile, exchange, engine);
    exchange.nextWithContext(renderer, delegate);
  }
}
