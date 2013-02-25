package org.ratpackframework.render;

/**
 * Converts the object to a string by simply calling its {@code toString()} method.
 */
public class DefaultTextRenderer implements TextRenderer {

  @Override
  public String render(Object object) {
    return object == null ? "null" : object.toString();
  }

}
