package org.ratpackframework.groovy.templating;

import com.google.inject.AbstractModule;
import org.ratpackframework.templating.TemplateRenderer;

public class TemplatingModule extends AbstractModule {

  private final TemplatingConfig templatingConfig;

  public TemplatingModule(TemplatingConfig templatingConfig) {
    this.templatingConfig = templatingConfig;
  }

  public TemplatingConfig getConfig() {
    return templatingConfig;
  }

  @Override
  protected void configure() {
    bind(TemplateRenderer.class).to(GroovyTemplateRenderer.class);
    bind(TemplatingConfig.class).toInstance(templatingConfig);
  }

}
