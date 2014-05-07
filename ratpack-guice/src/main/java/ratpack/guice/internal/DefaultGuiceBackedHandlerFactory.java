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
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Transformer;
import ratpack.guice.BindingsSpec;
import ratpack.guice.GuiceBackedHandlerFactory;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.handling.internal.FactoryHandler;
import ratpack.launch.LaunchConfig;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;

import javax.inject.Provider;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultGuiceBackedHandlerFactory implements GuiceBackedHandlerFactory {

  private final LaunchConfig launchConfig;

  public DefaultGuiceBackedHandlerFactory(LaunchConfig launchConfig) {
    this.launchConfig = launchConfig;
  }

  public Handler create(final Action<? super BindingsSpec> modulesAction, final Transformer<? super Module, ? extends Injector> moduleTransformer, final Transformer<? super Injector, ? extends Handler> injectorTransformer) throws Exception {
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

  private InjectorBindingHandler doCreate(Action<? super BindingsSpec> modulesAction, Transformer<? super Module, ? extends Injector> moduleTransformer, Transformer<? super Injector, ? extends Handler> injectorTransformer) throws Exception {
    DefaultBindingsSpec moduleRegistry = new DefaultBindingsSpec(launchConfig);

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

    decorated = Handlers.chain(decorateHandler(decorated), Handlers.notFound());

    return new InjectorBindingHandler(injector, decorated);
  }

  protected Handler decorateHandler(Handler handler) {
    return handler;
  }

  protected void registerDefaultModules(BindingsSpec bindingsSpec) {
    bindingsSpec.add(new DefaultRatpackModule(launchConfig));
  }

  private static class DefaultBindingsSpec implements BindingsSpec {

    private final List<Module> modules = new LinkedList<>();
    private final LaunchConfig launchConfig;

    private final LinkedList<Action<Binder>> actions = new LinkedList<>();
    private final LinkedList<Action<Injector>> init = new LinkedList<>();

    public DefaultBindingsSpec(LaunchConfig launchConfig) {
      this.launchConfig = launchConfig;
    }

    public LaunchConfig getLaunchConfig() {
      return launchConfig;
    }

    @Override
    public void bind(final Class<?> type) {
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) throws Exception {
          binder.bind(type);
        }
      });
    }

    @Override
    public <T> void bind(final Class<T> publicType, final Class<? extends T> implType) {
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) throws Exception {
          binder.bind(publicType).to(implType);
        }
      });
    }

    @Override
    public <T> void bind(final Class<? super T> publicType, final T instance) {
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) throws Exception {
          binder.bind(publicType).toInstance(instance);
        }
      });
    }

    @Override
    public <T> void bind(final T instance) {
      @SuppressWarnings("unchecked") final
      Class<T> type = (Class<T>) instance.getClass();
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) throws Exception {
          binder.bind(type).toInstance(instance);
        }
      });
    }

    @Override
    public <T> void provider(final Class<T> publicType, final Class<? extends Provider<? extends T>> providerType) {
      actions.add(new Action<Binder>() {
        public void execute(Binder binder) throws Exception {
          binder.bind(publicType).toProvider(providerType);
        }
      });
    }

    @Override
    public void add(Module... modules) {
      Collections.addAll(this.modules, modules);
    }

    @Override
    public void add(Iterable<? extends Module> modules) {
      for (Module module : modules) {
        this.modules.add(module);
      }
    }

    @Override
    public <T extends Module> T config(Class<T> moduleClass) {
      for (Module module : modules) {
        if (moduleClass.isInstance(module)) {
          return moduleClass.cast(module);
        }
      }

      return null;
    }

    private List<Module> getModules() {
      return ImmutableList.<Module>builder()
        .addAll(modules)
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
