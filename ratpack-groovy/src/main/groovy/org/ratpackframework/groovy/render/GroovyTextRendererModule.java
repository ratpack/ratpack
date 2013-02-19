package org.ratpackframework.groovy.render;

import com.google.inject.AbstractModule;
import org.ratpackframework.render.TextRenderer;

public class GroovyTextRendererModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(TextRenderer.class).to(GroovyTextRenderer.class);
  }

}
