package org.ratpackframework.templating;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.ratpackframework.templating.internal.Render;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TemplateRenderer {

  private final String templateDirPath;
  private final Vertx vertx;
  private final String errorPageTemplate;

  public TemplateRenderer(Vertx vertx, File dir) {
    this.vertx = vertx;
    this.templateDirPath = dir.getAbsolutePath();
    this.errorPageTemplate = getResourceText("exception.html");
  }

  public void renderFileTemplate(final String templateFileName, final Map<String, Object> model, final AsyncResultHandler<Buffer> handler) {
    vertx.fileSystem().readFile(getTemplatePath(templateFileName), new AsyncResultHandler<Buffer>() {
      @Override
      public void handle(AsyncResult<Buffer> event) {
        if (event.failed()) {
          handler.handle(new AsyncResult<Buffer>(event.exception));
        } else {
          render(event.result, templateFileName, model, handler);
        }
      }
    });
  }

  public void renderError(Map<String, Object> model, AsyncResultHandler<Buffer> handler) {
    render(errorPageTemplate, "errorpage", model, handler);
  }

  private void render(String template, String templateName, Map<String, Object> model, AsyncResultHandler<Buffer> handler) {
    render(new Buffer(template), templateName, model, handler);
  }

  private void render(Buffer template, String templateName, Map<String, Object> model, AsyncResultHandler<Buffer> handler) {
    new Render(new GroovyClassLoader(), vertx.fileSystem(), templateDirPath, template, templateName, model, handler);
  }

  private String getTemplatePath(String templateName) {
    return templateDirPath + File.separator + templateName;
  }

  private String getResourceText(String resourceName) {
    try {
      return IOGroovyMethods.getText(getClass().getResourceAsStream(resourceName));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
