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

package org.ratpackframework.guice.internal;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.ratpackframework.guice.HandlerDecoratingModule;
import org.ratpackframework.guice.ModuleRegistry;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.launch.LaunchConfig;
import org.ratpackframework.util.Action;
import org.ratpackframework.util.Transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultGuiceBackedHandlerFactory implements GuiceBackedHandlerFactory {

  private final LaunchConfig launchConfig;

  public DefaultGuiceBackedHandlerFactory(LaunchConfig launchConfig) {
    this.launchConfig = launchConfig;
  }

  public Handler create(Action<? super ModuleRegistry> modulesAction, Transformer<? super Injector, ? extends Handler> injectorTransformer) {
    DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry();

    registerDefaultModules(moduleRegistry);
    modulesAction.execute(moduleRegistry);

    Module masterModule = null;
    List<? extends Module> modules = moduleRegistry.getModules();
    for (Module module : modules) {
      if (masterModule == null) {
        masterModule = module;
      } else {
        masterModule = Modules.override(masterModule).with(module);
      }
    }

    Injector injector;
    if (masterModule == null) {
      injector = Guice.createInjector();
    } else {
      injector = Guice.createInjector(masterModule);
    }

    Handler decorated = injectorTransformer.transform(injector);

    List<Module> modulesReversed = new ArrayList<Module>(modules);
    Collections.reverse(modulesReversed);

    for (Module module : modulesReversed) {
      if (module instanceof HandlerDecoratingModule) {
        decorated = ((HandlerDecoratingModule) module).decorate(injector, decorated);
      }
    }

    decorated = decorateHandler(decorated);

    return new InjectorBindingHandler(injector, decorated);
  }

  protected Handler decorateHandler(Handler handler) {
    return handler;
  }

  protected void registerDefaultModules(ModuleRegistry moduleRegistry) {
    moduleRegistry.register(new DefaultRatpackModule(launchConfig));
  }

}
