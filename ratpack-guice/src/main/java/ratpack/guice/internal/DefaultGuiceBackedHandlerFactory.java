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

package ratpack.guice.internal;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.guice.ModuleRegistry;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.handling.internal.ClientErrorForwardingHandler;
import ratpack.launch.LaunchConfig;
import ratpack.util.Action;
import ratpack.util.Transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultGuiceBackedHandlerFactory implements GuiceBackedHandlerFactory {

  private final LaunchConfig launchConfig;

  public DefaultGuiceBackedHandlerFactory(LaunchConfig launchConfig) {
    this.launchConfig = launchConfig;
  }

  public Handler create(Action<? super ModuleRegistry> modulesAction, Transformer<? super Module, ? extends Injector> moduleTransformer, Transformer<? super Injector, ? extends Handler> injectorTransformer) {
    DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry(launchConfig);

    registerDefaultModules(moduleRegistry);
    try {
      modulesAction.execute(moduleRegistry);
    } catch (Exception e) {
      throw uncheck(e);
    }

    Module masterModule = null;
    List<? extends Module> modules = moduleRegistry.getModules();
    for (Module module : modules) {
      if (masterModule == null) {
        masterModule = module;
      } else {
        masterModule = Modules.override(masterModule).with(module);
      }
    }

    Injector injector = moduleTransformer.transform(masterModule);
    Handler decorated = injectorTransformer.transform(injector);

    List<Module> modulesReversed = new ArrayList<>(modules);
    Collections.reverse(modulesReversed);

    for (Module module : modulesReversed) {
      if (module instanceof HandlerDecoratingModule) {
        decorated = ((HandlerDecoratingModule) module).decorate(injector, decorated);
      }
    }

    decorated = Handlers.chain(decorateHandler(decorated), new ClientErrorForwardingHandler(404));

    return new InjectorBindingHandler(injector, decorated);
  }

  protected Handler decorateHandler(Handler handler) {
    return handler;
  }

  protected void registerDefaultModules(ModuleRegistry moduleRegistry) {
    moduleRegistry.register(new DefaultRatpackModule(launchConfig));
  }

}
