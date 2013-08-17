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

package org.ratpackframework.guice;

import com.google.inject.Injector;
import com.google.inject.Module;
import org.ratpackframework.guice.internal.DefaultGuiceBackedHandlerFactory;
import org.ratpackframework.guice.internal.InjectorBackedChildRegistry;
import org.ratpackframework.guice.internal.JustInTimeInjectorChildRegistry;
import org.ratpackframework.guice.internal.JustInTimeInjectorRegistry;
import org.ratpackframework.handling.Chain;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.launch.LaunchConfig;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.util.Action;
import org.ratpackframework.util.Transformer;
import org.ratpackframework.util.internal.ConstantTransformer;

import static com.google.inject.Guice.createInjector;
import static org.ratpackframework.handling.Handlers.chain;

/**
 * Utilities for creating Guice backed handlers.
 */
public abstract class Guice {

  private Guice() {
  }

  /**
   * Creates a handler that can be used as the entry point for a Guice backed Ratpack app.
   * <p>
   * Typically used as the “root” handler for an app.
   * <pre class="tested">
   * import org.ratpackframework.handling.*;
   * import org.ratpackframework.guice.*;
   * import org.ratpackframework.launch.*;
   * import org.ratpackframework.util.*;
   * import com.google.inject.*;
   * import javax.inject.*;
   *
   * class SomeService {}
   *
   * {@literal @}Singleton
   * class InjectedHandler implements Handler {
   *   private final SomeService service;
   *
   *   {@literal @}Inject
   *   public InjectedHandler(SomeService service) {
   *     this.service = service;
   *   }
   *
   *   public void handle(Context exchange) {
   *     // …
   *   }
   * }
   *
   * class ServiceModule extends AbstractModule {
   *   protected void configure() {
   *     bind(SomeService.class);
   *   }
   * }
   *
   * class ModuleBootstrap implements Action&lt;ModuleRegistry&gt; {
   *   public void execute(ModuleRegistry modules) {
   *     modules.register(new ServiceModule());
   *   }
   * }
   *
   * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("appRoot"))
   *   .build(new HandlerFactory() {
   *     public Handler create(LaunchConfig launchConfig) {
   *       return Guice.handler(launchConfig, new ModuleBootstrap(), new Action&lt;Chain&gt;() {
   *         public void execute(Chain chain) {
   *           chain.add(chain.getRegistry().get(InjectedHandler.class));
   *         }
   *       });
   *     }
   *   });
   * </pre>
   * <p>
   * Modules are processed eagerly. Before this method returns, the modules will be used to create a single
   * {@link com.google.inject.Injector} instance. This injector is available to the given handler via service lookup.
   * A new injector <b>is not</b> created for each exchange (as this would be unnecessary and expensive).
   * <p>
   * The injector also makes all of its objects available via service lookup.
   * This means that you can retrieve objects that were bound by modules in handlers via {@link org.ratpackframework.handling.Context#get(Class)}.
   * Objects are only retrievable via their public type.
   *
   * @param launchConfig The launch config of the server
   * @param moduleConfigurer The configurer of the {@link ModuleRegistry} to back the created handler
   * @param handler The handler of the application
   * @return A handler that makes the injector and its content available to the given handler
   */
  public static Handler handler(LaunchConfig launchConfig, Action<? super ModuleRegistry> moduleConfigurer, Handler handler) {
    return handler(launchConfig, moduleConfigurer, new ConstantTransformer<>(handler));
  }

  public static Handler handler(LaunchConfig launchConfig, Action<? super ModuleRegistry> moduleConfigurer, Transformer<? super Injector, ? extends Handler> injectorTransformer) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig).create(moduleConfigurer, newInjectorFactory(), injectorTransformer);
  }

  public static Handler handler(LaunchConfig launchConfig, Injector parentInjector, Action<? super ModuleRegistry> moduleConfigurer, Transformer<? super Injector, ? extends Handler> injectorTransformer) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig).create(moduleConfigurer, childInjectorFactory(parentInjector), injectorTransformer);
  }

  public static Handler handler(LaunchConfig launchConfig, Action<? super ModuleRegistry> moduleConfigurer, final Action<? super Chain> action) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig).create(moduleConfigurer, newInjectorFactory(), new InjectorHandlerTransformer(action));
  }

  public static Handler handler(LaunchConfig launchConfig, Injector parentInjector, Action<? super ModuleRegistry> moduleConfigurer, final Action<? super Chain> action) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig).create(moduleConfigurer, childInjectorFactory(parentInjector), new InjectorHandlerTransformer(action));
  }

  public static Registry<Object> justInTimeRegistry(Injector injector) {
    return new JustInTimeInjectorRegistry(injector);
  }

  public static Registry<Object> justInTimeRegistry(Registry<Object> parent, Injector injector) {
    return new JustInTimeInjectorChildRegistry(parent, injector);
  }

  public static Registry<Object> registry(Registry<Object> parent, Injector injector) {
    return new InjectorBackedChildRegistry(parent, injector);
  }

  public static Transformer<Module, Injector> newInjectorFactory() {
    return new Transformer<Module, Injector>() {
      @Override
      public Injector transform(Module from) {
        return from == null ? createInjector() : createInjector(from);
      }
    };
  }

  public static Transformer<Module, Injector> childInjectorFactory(final Injector parent) {
    return new Transformer<Module, Injector>() {
      @Override
      public Injector transform(Module from) {
        return from == null ? parent.createChildInjector() : parent.createChildInjector(from);
      }
    };
  }

  private static class InjectorHandlerTransformer implements Transformer<Injector, Handler> {
    private final Action<? super Chain> action;

    public InjectorHandlerTransformer(Action<? super Chain> action) {
      this.action = action;
    }

    public Handler transform(Injector injector) {
      return chain(Guice.justInTimeRegistry(injector), action);
    }
  }
}
