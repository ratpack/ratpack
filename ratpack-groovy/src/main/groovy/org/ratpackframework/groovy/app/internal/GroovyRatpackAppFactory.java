package org.ratpackframework.groovy.app.internal;

import org.ratpackframework.groovy.bootstrap.ModuleRegistry;
import org.ratpackframework.groovy.templating.TemplatingConfig;
import org.ratpackframework.groovy.templating.TemplatingModule;

import java.io.File;

public class GroovyRatpackAppFactory extends ClosureAppFactory {

  private final File templatesDir;

  public GroovyRatpackAppFactory(File templatesDir) {
    this.templatesDir = templatesDir;
  }

  @Override
  void registerExtraModules(ModuleRegistry modules) {
    modules.register(new TemplatingModule(new TemplatingConfig(templatesDir)));
  }

}
