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

package ratpack.guice;

import com.google.common.collect.Lists;
import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.func.Pair;
import ratpack.guice.internal.*;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.Handlers;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;

import java.util.Collections;
import java.util.List;

import static com.google.inject.Guice.createInjector;
import static ratpack.util.ExceptionUtils.uncheck;

/**
 * Static utility methods for creating Google Guice based Ratpack infrastructure.
 * <p>
 * Any non trivial application will require supporting objects for the handler implementations, for persistence for example.
 * These supporting objects can be managed by Guice and made available to handlers (either by dependency injection or registry lookup)
 * for increased reusability, modularity and testability.
 * <p>
 * The Guice integration is not part of the Ratpack core library, but is available as a separate add on.
 * That said, Ratpack is designed to support something like Guice as an integral piece via it's {@link Registry} abstraction.
 * Guice is the “official” solution.
 * </p>
 * <h3>Starting a Guice Ratpack app</h3>
 * <p>
 * Below is a complete example for bootstrapping a Guice based Ratpack application.
 * </p>
 * <pre class="java">{@code
 * import com.google.inject.AbstractModule;
 * import ratpack.guice.Guice;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import javax.inject.Inject;
 * import javax.inject.Singleton;
 *
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   static class SomeService {
 *     private final String value;
 *
 *     SomeService(String value) {
 *       this.value = value;
 *     }
 *
 *     public String getValue() {
 *       return value;
 *     }
 *   }
 *
 *   static class SomeOtherService {
 *     private final String value;
 *
 *     SomeOtherService(String value) {
 *       this.value = value;
 *     }
 *
 *     public String getValue() {
 *       return value;
 *     }
 *   }
 *
 *   static class ServiceModule extends AbstractModule {
 *     protected void configure() {
 *       bind(SomeService.class).toInstance(new SomeService("foo"));
 *       bind(SomeOtherService.class).toInstance(new SomeOtherService("bar"));
 *     }
 *   }
 *
 *   {@literal @}Singleton
 *   static class InjectedHandler implements Handler {
 *     private final SomeService service;
 *
 *     {@literal @}Inject
 *     public InjectedHandler(SomeService service) {
 *       this.service = service;
 *     }
 *
 *     public void handle(Context ctx) {
 *       ctx.render(service.getValue() + "-" + ctx.get(SomeOtherService.class).getValue());
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlerFactory(registry ->
 *         Guice.builder(registry)
 *           .bindings(b -> b.add(new ServiceModule()))
 *           .build(chain -> {
 *             // The registry in a Guice backed chain can be used to retrieve objects that were bound,
 *             // or to create objects that are bound “just-in-time”.
 *             chain.get("some/path", InjectedHandler.class);
 *           })
 *     ).test(httpClient -> {
 *       assertEquals("foo-bar", httpClient.get("some/path").getBody().getText());
 *     });
 *   }
 *
 * }
 * }</pre>
 * <h3>Accessing Guice bound objects in Handlers</h3>
 * <p>
 * There are two ways to use Guice bound objects in your handler implementations.
 * </p>
 * <h4>Dependency Injected Handlers</h4>
 * <p>
 * The {@code handler()} methods used to create a Guice backed application take an {@link Action} that operates on a {@link Chain} instance.
 * This chain instance given to this action provides a Guice backed {@link Registry} via its {@link Chain#getRegistry()} method.
 * This registry is able to retrieve objects that were explicitly bound (i.e. defined by a module), and bind objects “just in time”.
 * This means that it can be used to construct dependency injected {@link Handler} implementations.
 * </p>
 * <p>
 * Simply pass the class of the handler implementation to create a new dependency injected instance of to this method,
 * then add it to the chain.
 * </p>
 * <p>
 * See the code above for an example of this.
 * </p>
 * <h4>Accessing dependencies via context registry lookup</h4>
 * <p>
 * The {@link ratpack.handling.Context} object that is given to a handler's {@link Handler#handle(ratpack.handling.Context)}
 * method is also a registry implementation. In a Guice backed app, Guice bound objects can be retrieved via the {@link Registry#get(Class)} method of
 * the context. You can retrieve any bound object via its publicly bound type.
 * </p>
 * <p>
 * However, this will not create “just-in-time” bindings. Only objects that were explicitly bound can be retrieved this way.
 * </p>
 * <h3>Guice modules as Ratpack “plugins”.</h3>
 * <p>
 * Add on Ratpack functionality is typically provided via Guice modules.
 * For example, the <a href="https://github.com/FasterXML/jackson-databind">Jackson</a> integration for JSON serialisation
 * is provided by the {@code ratpack-jackson} add-on which ships a Guice module.
 * To use its functionality simply register the module it provides with the {@link BindingsSpec} used to bootstrap the application.
 * </p>
 * <h3>Groovy Applications</h3>
 * <p>
 * The Ratpack Groovy add-on provides application modes that automatically incorporate Guice (namely the “Ratpack Script” mode).
 * The module registration process is simpler and more convenient in this mode, and there are additional options for obtaining
 * Guice bound objects.
 * </p>
 * <p>
 * See the Groovy add-on's documentation for more details.
 * </p>
 */
public abstract class Guice {

  private Guice() {
  }

  public interface Builder {
    Builder parent(Injector injector);

    Builder bindings(Action<? super BindingsSpec> action);

    Handler build(Action<? super Chain> action) throws Exception;
  }

  public static Builder builder(Registry rootRegistry) {
    return new Builder() {
      private Injector parent;
      private Action<? super BindingsSpec> bindings = Action.noop();

      @Override
      public Builder parent(Injector injector) {
        parent = injector;
        return this;
      }

      @Override
      public Builder bindings(Action<? super BindingsSpec> action) {
        bindings = action;
        return this;
      }

      @Override
      public Handler build(Action<? super Chain> action) throws Exception {
        Function<Module, Injector> moduleTransformer = parent == null ? newInjectorFactory(rootRegistry.get(ServerConfig.class)) : childInjectorFactory(parent);
        return new DefaultGuiceBackedHandlerFactory(rootRegistry).create(bindings, moduleTransformer,
          injector -> Handlers.chain(rootRegistry.get(ServerConfig.class), justInTimeRegistry(injector), action)
        );
      }
    };
  }

  /**
   * Creates a Ratpack {@link Registry} backed by the given {@link Injector} that will create objects via “just-in-time” binding.
   * <p>
   * The returned registry differs from the registry returned by {@link #registry(com.google.inject.Injector)} in that it may return objects that were not explicitly bound,
   * via Guice's “just-in-time” binding mechanism.
   * This only applies to the {@link Registry#get(Class)} and {@link Registry#maybeGet(Class)} (for all overloads).
   * All other methods do not perform any just in time bindings.
   *
   * @param injector The injector to back the registry
   * @return A registry that wraps the injector
   */
  public static Registry justInTimeRegistry(Injector injector) {
    return new JustInTimeInjectorRegistry(injector);
  }

  /**
   * Creates a Ratpack {@link Registry} backed by the given {@link Injector} that will <b>NOT</b> create objects via “just-in-time” binding.
   *
   * @param injector The injector to back the registry
   * @return A registry that wraps the injector
   */
  public static Registry registry(Injector injector) {
    return Registries.backedRegistry(new InjectorRegistryBacking(injector));
  }

  public static Function<Registry, Registry> registry(Action<? super BindingsSpec> bindings) {
    return baseRegistry -> registry(bindings, baseRegistry, newInjectorFactory(baseRegistry.get(ServerConfig.class)));
  }

  public static Function<Registry, Registry> registry(Injector parentInjector, Action<? super BindingsSpec> bindings) {
    return baseRegistry -> registry(bindings, baseRegistry, childInjectorFactory(parentInjector));
  }

  private static Registry registry(Action<? super BindingsSpec> bindings, Registry baseRegistry, Function<Module, Injector> injectorFactory) throws Exception {
    Pair<List<Module>, Injector> pair = buildInjector(baseRegistry, bindings, injectorFactory);
    return registry(pair.right);
  }

  public static Function<Module, Injector> newInjectorFactory(final ServerConfig serverConfig) {
    final Stage stage = serverConfig.isDevelopment() ? Stage.DEVELOPMENT : Stage.PRODUCTION;
    return from -> from == null ? createInjector(stage) : createInjector(stage, from);
  }

  private static Function<Module, Injector> childInjectorFactory(final Injector parent) {
    return from -> from == null ? parent.createChildInjector() : parent.createChildInjector(from);
  }

  private static Pair<List<Module>, Injector> buildInjector(Registry baseRegistry, Action<? super BindingsSpec> bindingsAction, Function<? super Module, ? extends Injector> injectorFactory) throws Exception {
    List<Action<? super Binder>> binderActions = Lists.newLinkedList();
    List<Module> modules = Lists.newLinkedList();

    BindingsSpec bindings = new DefaultBindingsSpec(baseRegistry.get(ServerConfig.class), binderActions, modules);
    bindings.add(new RatpackBaseRegistryModule(baseRegistry));

    try {
      bindingsAction.execute(bindings);
    } catch (Exception e) {
      throw uncheck(e);
    }

    modules.add(new AdHocModule(binderActions));

    Module masterModule = modules.stream().reduce((acc, next) -> Modules.override(acc).with(next)).get();
    Injector injector = injectorFactory.apply(masterModule);

    List<HandlerDecorator> adaptedDecorators = Lists.newLinkedList();
    Collections.reverse(modules);
    for (Module module : modules) {
      if (module instanceof HandlerDecoratingModule) {
        adaptedDecorators.add(new ModuleAdaptedHandlerDecorator((HandlerDecoratingModule) module, injector));
      }
    }

    if (!adaptedDecorators.isEmpty()) {
      injector = injector.createChildInjector(new AbstractModule() {
        @Override
        protected void configure() {
          Multibinder<HandlerDecorator> multiBinder = Multibinder.newSetBinder(binder(), HandlerDecorator.class);
          for (HandlerDecorator adaptedDecorator : adaptedDecorators) {
            multiBinder.addBinding().toInstance(adaptedDecorator);
          }
        }
      });
    }

    return Pair.of(modules, injector);
  }

  private static class ModuleAdaptedHandlerDecorator implements HandlerDecorator {

    private final HandlerDecoratingModule module;
    private final Injector injector;

    public ModuleAdaptedHandlerDecorator(HandlerDecoratingModule module, Injector injector) {
      this.module = module;
      this.injector = injector;
    }

    @Override
    public Handler decorate(Registry serverRegistry, Handler rest) {
      return module.decorate(injector, rest);
    }
  }

  private static class AdHocModule implements Module {

    private final List<Action<? super Binder>> binderActions;

    public AdHocModule(List<Action<? super Binder>> binderActions) {
      this.binderActions = binderActions;
    }

    @Override
    public void configure(Binder binder) {
      for (Action<? super Binder> binderAction : binderActions) {
        binderAction.toConsumer().accept(binder);
      }
    }
  }
}
