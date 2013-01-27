package org.ratpackframework.templating.internal;

import org.vertx.java.core.buffer.Buffer;

import java.util.List;

public class ExecutedTemplate {

  private final List<Object> parts;

  public ExecutedTemplate(List<Object> parts) {
    this.parts = parts;
  }

  public List<Object> getParts() {
    return parts;
  }

  void render(Buffer buffer) {
    for (Object part : parts) {
      if (part instanceof NestedTemplate) {
        ((NestedTemplate) part).render(buffer);
      } else {
        buffer.appendString(part.toString());
      }
    }
  }

}
