package org.ratpackframework.groovy.templating;

import com.google.inject.AbstractModule;
import org.ratpackframework.groovy.error.ErrorHandler;
import org.ratpackframework.groovy.error.NotFoundHandler;
import org.ratpackframework.templating.TemplateRenderer;

import static com.google.inject.name.Names.named;
import static org.ratpackframework.bootstrap.internal.RootModule.*;

public class TemplatingModule extends AbstractModule {

  private final TemplatingConfig templatingConfig;

  public TemplatingModule(TemplatingConfig templatingConfig) {
    this.templatingConfig = templatingConfig;
  }

  @Override
  protected void configure() {
    bind(HTTP_ERROR_HANDLER).annotatedWith(named(MAIN_HTTP_ERROR_HANDLER)).to(ErrorHandler.class);
    bind(HTTP_HANDLER).annotatedWith(named(MAIN_NOT_FOUND_HTTP_HANDLER)).to(NotFoundHandler.class);
    bind(TemplateRenderer.class).to(GroovyTemplateRenderer.class);
    bind(TemplatingConfig.class).toInstance(templatingConfig);
  }

}
