package org.ratpackframework.render;

public class DefaultTextRenderer implements TextRenderer {

  @Override
  public String render(Object object) {
    return object == null ? "null" : object.toString();
  }

}
