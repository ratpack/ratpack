package org.ratpackframework.groovy.render;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.ratpackframework.render.TextRenderer;

public class GroovyTextRenderer implements TextRenderer {

  @Override
  public String render(Object object) {
    return DefaultGroovyMethods.toString(object);
  }

}
