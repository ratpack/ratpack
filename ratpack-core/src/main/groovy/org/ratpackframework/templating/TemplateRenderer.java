package org.ratpackframework.templating;

import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.buffer.Buffer;

import java.util.Map;

public interface TemplateRenderer {
  void renderFileTemplate(String templateFileName, Map<String, ?> model, AsyncResultHandler<Buffer> handler);

  void renderError(Map<String, ?> model, AsyncResultHandler<Buffer> handler);
}
