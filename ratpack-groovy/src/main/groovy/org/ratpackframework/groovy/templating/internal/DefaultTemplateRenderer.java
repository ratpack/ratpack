package org.ratpackframework.groovy.templating.internal;

import io.netty.buffer.ByteBuf;
import org.ratpackframework.Result;
import org.ratpackframework.ResultAction;
import org.ratpackframework.groovy.templating.TemplateRenderer;
import org.ratpackframework.routing.Exchange;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class DefaultTemplateRenderer implements TemplateRenderer {

  private final File templateDir;
  private final Exchange exchange;
  private final GroovyTemplateRenderingEngine engine;

  public DefaultTemplateRenderer(File templateDir, Exchange exchange, GroovyTemplateRenderingEngine engine) {
    this.templateDir = templateDir;
    this.exchange = exchange;
    this.engine = engine;
  }

  @Override
  public void render(Map<String, ?> model, String templateId) {
    engine.renderTemplate(templateDir, templateId, model, new ResultAction<ByteBuf>() {
      @Override
      public void execute(Result<ByteBuf> event) {
        if (event.isFailure()) {
          error(ExceptionToTemplateModel.transform(exchange.getRequest(), event.getFailure()));
        } else {
          exchange.getResponse().send("text/html", event.getValue());
        }
      }
    });
  }

  public void render(String templateId) {
    render(Collections.<String, Object>emptyMap(), templateId);
  }

  @Override
  public void error(Map<String, ?> model) {
    engine.renderError(templateDir, model, new ResultAction<ByteBuf>() {
      @Override
      public void execute(Result<ByteBuf> event) {
        if (event.isFailure()) {
          exchange.error(event.getFailure());
        } else {
          exchange.getResponse().status(500).send("text/html", event.getValue());
        }
      }
    });
  }

}
