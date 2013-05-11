/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
