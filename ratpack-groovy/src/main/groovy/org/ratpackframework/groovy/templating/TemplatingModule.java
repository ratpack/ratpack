package org.ratpackframework.groovy.templating;

import com.google.inject.AbstractModule;
import org.ratpackframework.groovy.templating.internal.GroovyTemplateRenderingEngine;

public class TemplatingModule extends AbstractModule {

  private final TemplatingConfig templatingConfig = new TemplatingConfig();

  public TemplatingConfig getConfig() {
    return templatingConfig;
  }

  @Override
  protected void configure() {
    bind(GroovyTemplateRenderingEngine.class);
    bind(TemplatingConfig.class).toInstance(templatingConfig);
  }

}
