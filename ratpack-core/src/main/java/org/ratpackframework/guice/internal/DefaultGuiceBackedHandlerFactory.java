package org.ratpackframework.guice.internal;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.ratpackframework.Action;
import org.ratpackframework.guice.GuiceBackedHandlerFactory;
import org.ratpackframework.guice.ModuleRegistry;
import org.ratpackframework.routing.Handler;

public class DefaultGuiceBackedHandlerFactory implements GuiceBackedHandlerFactory {

  @Override
  public Handler create(Action<? super ModuleRegistry> modulesAction, Handler handler) {
    ModuleRegistry modules = new DefaultModuleRegistry();

    registerDefaultModules(modules);
    modulesAction.execute(modules);

    Module masterModule = null;
    for (Module module : modules.getModules()) {
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

    Handler decorated = decorateHandler(handler);
    return new InjectorBindingHandler(injector, decorated);
  }

  protected Handler decorateHandler(Handler handler) {
    return handler;
  }

  protected void registerDefaultModules(ModuleRegistry moduleRegistry) {

  }

}
