package org.ratpackframework.templating;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class TemplateCompiler {

  private final String templateDirPath;
  private final Vertx vertx;
  private final String errorPageTemplate;

  public TemplateCompiler(Vertx vertx, File dir) {
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

  public void compileFileTemplate(String templateFileName, final Map<Object, Object> model, final AsyncResultHandler<CompiledTemplate> handler) {
    vertx.fileSystem().readFile(getTemplatePath(templateFileName), new AsyncResultHandler<Buffer>() {
      @Override
      public void handle(AsyncResult<Buffer> event) {
        if (event.failed()) {
          handler.handle(new AsyncResult<CompiledTemplate>(event.exception));
        } else {
          compile(event.result, model, handler);
        }
      }
    });
  }

  void compile(String template, Map<Object, Object> model, AsyncResultHandler<CompiledTemplate> handler) {
    compile(new Buffer(template), model, handler);
  }

  void compile(Buffer template, Map<Object, Object> model, AsyncResultHandler<CompiledTemplate> handler) {
    innerCompiling(new GroovyClassLoader(), template, model, handler);
  }

  protected void innerCompiling(ClassLoader classLoader, Buffer template, final Map<Object, Object> model, AsyncResultHandler<CompiledTemplate> handler) {
    SimpleTemplateEngine engine = new SimpleTemplateEngine(classLoader);
    Map<Object, Object> binding = new LinkedHashMap<Object, Object>();
    binding.put("model", model);

    Writable writable;
    try {
      writable = engine.createTemplate(template.toString()).make(binding);
    } catch (Exception e) {
      handler.handle(new AsyncResult<CompiledTemplate>(e));
      return;
    }

    handler.handle(new AsyncResult<CompiledTemplate>(new CompiledTemplate(writable)));
  }

  public void compileErrorTemplate(Map<Object, Object> model, AsyncResultHandler<CompiledTemplate> handler) {
    compile(errorPageTemplate, model, handler);
  }

  protected String getTemplatePath(String templateName) {
    return templateDirPath + File.separator + templateName;
  }

}
