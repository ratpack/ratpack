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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.guice.BindingsSpec;
import ratpack.guice.Guice;
import ratpack.guice.GuiceBackedHandlerFactory;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.handling.internal.FactoryHandler;
import ratpack.server.ServerConfig;
import ratpack.registry.Registry;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class DefaultGuiceBackedHandlerFactory implements GuiceBackedHandlerFactory {

  private final Registry rootRegistry;
  private final ServerConfig serverConfig;

  public DefaultGuiceBackedHandlerFactory(Registry rootRegistry) {
    this.rootRegistry = rootRegistry;
    this.serverConfig = rootRegistry.get(ServerConfig.class);
  }

  public Handler create(final Action<? super BindingsSpec> modulesAction, final Function<? super Module, ? extends Injector> moduleTransformer, final Function<? super Injector, ? extends Handler> injectorTransformer) throws Exception {
    if (serverConfig.isDevelopment()) {
      File classFile = ClassUtil.getClassFile(modulesAction);
      if (classFile != null) {
        Factory<Handler> factory = new ReloadableFileBackedFactory<>(classFile.toPath(), true, (file, bytes) -> doCreate(modulesAction, moduleTransformer, injectorTransformer));

        return new FactoryHandler(factory);
      }
    }

    return doCreate(modulesAction, moduleTransformer, injectorTransformer);
  }

  private Handler doCreate(Action<? super BindingsSpec> modulesAction, Function<? super Module, ? extends Injector> moduleTransformer, Function<? super Injector, ? extends Handler> injectorTransformer) throws Exception {

    List<Action<? super Binder>> binderActions = Lists.newLinkedList();
    List<Action<? super Injector>> injectorActions = Lists.newLinkedList();
    List<Module> modules = Lists.newLinkedList();

    BindingsSpec moduleRegistry = new DefaultBindingsSpec(serverConfig, binderActions, injectorActions, modules);

    registerDefaultModules(moduleRegistry);
    modulesAction.toConsumer().accept(moduleRegistry);

    Module overrideModule = binder -> {
      for (Action<? super Binder> binderAction : binderActions) {
        binderAction.toConsumer().accept(binder);
      }
    };

    List<Module> effectiveModules = ImmutableList.<Module>builder()
      .addAll(modules)
      .add(overrideModule)
      .build();

    Module masterModule = effectiveModules.stream().reduce((acc, next) -> Modules.override(acc).with(next)).get();
    Injector injector = moduleTransformer.apply(masterModule);

    for (Action<? super Injector> initAction : injectorActions) {
      initAction.execute(injector);
    }

    Handler handler = injectorTransformer.apply(injector);
    Collections.reverse(modules);
    for (Module module : modules) {
      if (module instanceof HandlerDecoratingModule) {
        handler = ((HandlerDecoratingModule) module).decorate(injector, handler);
      }
    }

    Registry registry = Guice.registry(injector);
    return Handlers.chain(Handlers.register(registry), handler);
  }

  protected void registerDefaultModules(BindingsSpec bindingsSpec) {
    bindingsSpec.add(new DefaultRatpackModule(rootRegistry));
  }

}
