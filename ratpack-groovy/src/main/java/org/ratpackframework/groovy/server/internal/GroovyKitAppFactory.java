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

package org.ratpackframework.groovy.server.internal;

import org.ratpackframework.groovy.templating.TemplatingModule;
import org.ratpackframework.guice.ModuleRegistry;
import org.ratpackframework.guice.internal.DefaultGuiceBackedHandlerFactory;
import org.ratpackframework.session.SessionModule;

public class GroovyKitAppFactory extends DefaultGuiceBackedHandlerFactory {

  @Override
  protected void registerDefaultModules(ModuleRegistry moduleRegistry) {
    moduleRegistry.register(new SessionModule());
    moduleRegistry.register(new TemplatingModule());
    super.registerDefaultModules(moduleRegistry);
  }

}
