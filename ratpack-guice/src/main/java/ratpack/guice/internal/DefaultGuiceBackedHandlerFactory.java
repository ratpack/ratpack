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
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import io.netty.buffer.ByteBuf;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.guice.ModuleRegistry;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.handling.internal.ClientErrorForwardingHandler;
import ratpack.handling.internal.FactoryHandler;
import ratpack.launch.LaunchConfig;
import ratpack.registry.NotInRegistryException;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.util.Action;
import ratpack.util.Factory;
import ratpack.util.Transformer;

import javax.inject.Provider;
import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultGuiceBackedHandlerFactory implements GuiceBackedHandlerFactory {

  private final LaunchConfig launchConfig;

  public DefaultGuiceBackedHandlerFactory(LaunchConfig launchConfig) {
    this.launchConfig = launchConfig;
  }

  public Handler create(final Action<? super ModuleRegistry> modulesAction, final Transformer<? super Module, ? extends Injector> moduleTransformer, final Transformer<? super Injector, ? extends Handler> injectorTransformer) throws Exception {
    if (launchConfig.isReloadable()) {
      File classFile = ClassUtil.getClassFile(modulesAction);
      if (classFile != null) {
        Factory<InjectorBindingHandler> factory = new ReloadableFileBackedFactory<>(classFile.toPath(), true, new ReloadableFileBackedFactory.Producer<InjectorBindingHandler>() {
          @Override
          public InjectorBindingHandler produce(Path file, ByteBuf bytes) throws Exception {
            return doCreate(modulesAction, moduleTransformer, injectorTransformer);
          }
        });

        return new FactoryHandler(factory);
      }
    }

    return doCreate(modulesAction, moduleTransformer, injectorTransformer);
  }

  private InjectorBindingHandler doCreate(Action<? super ModuleRegistry> modulesAction, Transformer<? super Module, ? extends Injector> moduleTransformer, Transformer<? super Injector, ? extends Handler> injectorTransformer) throws Exception {
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

    List<Action<Injector>> init = moduleRegistry.init;
    for (Action<Injector> initAction : init) {
      initAction.execute(injector);
    }

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

  private static class DefaultModuleRegistry implements ModuleRegistry {

    private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<>();
    private final LaunchConfig launchConfig;

    private final LinkedList<Action<Binder>> actions = new LinkedList<>();
    private final LinkedList<Action<Injector>> init = new LinkedList<>();

    public DefaultModuleRegistry(LaunchConfig launchConfig) {
      this.launchConfig = launchConfig;
    }

    public LaunchConfig getLaunchConfig() {
      return launchConfig;
    }

    public void bind(final Class<?> type) {
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) {
          binder.bind(type);
        }
      });
    }

    public <T> void bind(final Class<T> publicType, final Class<? extends T> implType) {
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) {
          binder.bind(publicType).to(implType);
        }
      });
    }

    public <T> void bind(final Class<? super T> publicType, final T instance) {
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) {
          binder.bind(publicType).toInstance(instance);
        }
      });
    }

    public <T> void bind(final T instance) {
      @SuppressWarnings("unchecked") final
      Class<T> type = (Class<T>) instance.getClass();
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) {
          binder.bind(type).toInstance(instance);
        }
      });
    }

    public <T> void provider(final Class<T> publicType, final Class<? extends Provider<? extends T>> providerType) {
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) {
          binder.bind(publicType).toProvider(providerType);
        }
      });
    }

    @SuppressWarnings("unchecked")
    public void register(Module module) {
      register((Class<Module>) module.getClass(), module);
    }

    public <O extends Module> void register(Class<O> type, O module) {
      if (modules.containsKey(type)) {
        Object existing = modules.get(type);
        throw new IllegalArgumentException(String.format("Module '%s' is already registered with type '%s' (attempting to register '%s')", existing, type, module));
      }

      modules.put(type, module);
    }

    public <T> T get(Class<T> moduleType) {
      @SuppressWarnings("SuspiciousMethodCalls") Module module = modules.get(moduleType);
      if (module == null) {
        throw new NotInRegistryException(moduleType);
      }

      return moduleType.cast(module);
    }

    @Override
    public <O> List<O> getAll(Class<O> type) {
      ImmutableList.Builder<O> builder = ImmutableList.builder();
      for (Module module : modules.values()) {
        if (type.isInstance(module)) {
          @SuppressWarnings("unchecked") O cast = (O) module;
          builder.add(cast);
        }
      }
      return builder.build();
    }

    public <O> O maybeGet(Class<O> type) {
      @SuppressWarnings("SuspiciousMethodCalls") Module module = modules.get(type);
      if (module == null) {
        return null;
      }

      return type.cast(module);
    }

    public <T extends Module> T remove(Class<T> moduleType) {
      if (!modules.containsKey(moduleType)) {
        throw new NotInRegistryException(moduleType);
      }

      return moduleType.cast(modules.remove(moduleType));
    }

    private List<Module> getModules() {
      return ImmutableList.<Module>builder()
        .addAll(modules.values())
        .add(createOverrideModule())
        .build();
    }

    private Module createOverrideModule() {
      return new Module() {
        public void configure(Binder binder) {
          for (Action<Binder> binderAction : actions) {
            try {
              binderAction.execute(binder);
            } catch (Exception e) {
              throw uncheck(e);
            }
          }
        }
      };
    }

    @Override
    public void init(Action<Injector> action) {
      init.add(action);
    }

    @Override
    public void init(final Class<? extends Runnable> clazz) {
      init.add(new Action<Injector>() {
        @Override
        public void execute(Injector injector) throws Exception {
          injector.getInstance(clazz).run();
        }
      });
    }

  }
}
