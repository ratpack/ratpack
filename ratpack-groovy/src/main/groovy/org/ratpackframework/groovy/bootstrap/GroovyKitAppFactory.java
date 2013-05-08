package org.ratpackframework.groovy.bootstrap;

import org.ratpackframework.file.internal.FileSystemContextHandler;
import org.ratpackframework.groovy.templating.TemplatingHandlers;
import org.ratpackframework.groovy.templating.TemplatingModule;
import org.ratpackframework.guice.ModuleRegistry;
import org.ratpackframework.guice.internal.DefaultGuiceBackedHandlerFactory;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.session.SessionModule;

import java.io.File;

public class GroovyKitAppFactory extends DefaultGuiceBackedHandlerFactory {

  private final File baseDir;

  public GroovyKitAppFactory(File baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  protected void registerDefaultModules(ModuleRegistry moduleRegistry) {
    moduleRegistry.register(new SessionModule());
    moduleRegistry.register(new TemplatingModule());

    super.registerDefaultModules(moduleRegistry);
  }

  @Override
  protected Handler decorateHandler(Handler handler) {
    return super.decorateHandler(
        new FileSystemContextHandler(baseDir,
            TemplatingHandlers.templates("templates", handler)
        )
    );
  }

}
