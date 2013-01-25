package org.ratpackframework.templating;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.runtime.IOGroovyMethods;
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

  private String getResourceText(String resourceName) {
    try {
      return IOGroovyMethods.getText(getClass().getResourceAsStream(resourceName));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void renderFileTemplate(String templateFileName, final Map<Object, Object> model, final AsyncResultHandler<Buffer> handler) {
    vertx.fileSystem().readFile(getTemplatePath(templateFileName), new AsyncResultHandler<Buffer>() {
      @Override
      public void handle(AsyncResult<Buffer> event) {
        if (event.failed()) {
          handler.handle(new AsyncResult<Buffer>(event.exception));
        } else {
          render(event.result, model, handler);
        }
      }
    });
  }

  void render(String template, Map<Object, Object> model, AsyncResultHandler<Buffer> handler) {
    render(new Buffer(template), model, handler);
  }

  void render(Buffer template, Map<Object, Object> model, AsyncResultHandler<Buffer> handler) {
    new InnerRenderer(new GroovyClassLoader(), vertx.fileSystem(), templateDirPath, template, model, handler);
  }

  public void renderError(Map<Object, Object> model, AsyncResultHandler<Buffer> handler) {
    render(errorPageTemplate, model, handler);
  }

  protected String getTemplatePath(String templateName) {
    return templateDirPath + File.separator + templateName;
  }

}
