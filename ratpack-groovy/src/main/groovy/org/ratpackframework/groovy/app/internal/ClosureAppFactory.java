package org.ratpackframework.groovy.app.internal;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import groovy.lang.Closure;
import org.ratpackframework.app.internal.AppModule;
import org.ratpackframework.app.internal.AppRoutingModule;
import org.ratpackframework.bootstrap.internal.RootModule;
import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.groovy.Closures;
import org.ratpackframework.groovy.app.ClosureRouting;
import org.ratpackframework.groovy.bootstrap.ModuleRegistry;
import org.ratpackframework.groovy.bootstrap.internal.DefaultModuleRegistry;
import org.ratpackframework.Action;
import org.ratpackframework.http.CoreHttpHandlers;
import org.ratpackframework.http.DefaultCoreHttpHandlers;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;
import org.ratpackframework.session.SessionModule;

public class ClosureAppFactory {

  public CoreHttpHandlers create(Closure<?> moduleConfigurer, Closure<?> routingConfigurer) {
    ModuleRegistry modules = new DefaultModuleRegistry();

    modules.register(new RootModule());
    modules.register(new SessionModule());
    modules.register(new AppModule());
    modules.register(AppRoutingModule.create(new ClosureRouting(routingConfigurer)));

    registerExtraModules(modules);

    Closures.configure(modules, moduleConfigurer);

    Module masterModule = null;
    for (Module module : modules.getModules()) {
      if (masterModule == null) {
        masterModule = module;
      } else {
        masterModule = Modules.override(masterModule).with(module);
      }
    }

    Injector injector = Guice.createInjector(masterModule);

    Key<Action<Routed<HttpExchange>>> appHandlerKey = Key.get(RootModule.HTTP_HANDLER, Names.named(RootModule.MAIN_APP_HTTP_HANDLER));
    Key<Action<ErroredHttpExchange>> errorHandlerKey = Key.get(RootModule.HTTP_ERROR_HANDLER, Names.named(RootModule.MAIN_HTTP_ERROR_HANDLER));
    Key<Action<Routed<HttpExchange>>> notFoundHandlerKey = Key.get(RootModule.HTTP_HANDLER, Names.named(RootModule.MAIN_NOT_FOUND_HTTP_HANDLER));

    Action<Routed<HttpExchange>> appHandler = injector.getInstance(appHandlerKey);
    Action<ErroredHttpExchange> errorHandler = injector.getInstance(errorHandlerKey);
    Action<Routed<HttpExchange>> notFoundHandler = injector.getInstance(notFoundHandlerKey);

    return new DefaultCoreHttpHandlers(appHandler, errorHandler, notFoundHandler);
  }

  void registerExtraModules(ModuleRegistry registry) {

  }

}
