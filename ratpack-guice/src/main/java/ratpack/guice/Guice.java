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
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import ratpack.config.ConfigObject;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.guice.internal.DefaultBindingsSpec;
import ratpack.guice.internal.GuiceUtil;
import ratpack.guice.internal.InjectorRegistryBacking;
import ratpack.guice.internal.RatpackBaseRegistryModule;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.impose.Impositions;
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;

import java.util.List;
import java.util.Optional;

import static com.google.inject.Guice.createInjector;
import static ratpack.util.Exceptions.uncheck;

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
 *   public static class ServiceModule extends AbstractModule {
 *     protected void configure() {
 *       bind(SomeService.class).toInstance(new SomeService("foo"));
 *       bind(SomeOtherService.class).toInstance(new SomeOtherService("bar"));
 *       bind(InjectedHandler.class);
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
 *     EmbeddedApp.of(s -> s
 *       .registry(Guice.registry(b -> b.module(ServiceModule.class)))
 *       .handlers(chain -> {
 *         // The registry in a Guice backed chain can be used to retrieve objects that were bound,
 *         // or to create objects that are bound “just-in-time”.
 *         chain.get("some/path", InjectedHandler.class);
 *       })
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
 * The {@link Context} object that is given to a handler's {@link Handler#handle(Context)}
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

  /**
   * Creates a Ratpack {@link Registry} backed by the given {@link Injector} that will <b>NOT</b> create objects via “just-in-time” binding.
   *
   * @param injector The injector to back the registry
   * @return A registry that wraps the injector
   */
  public static Registry registry(Injector injector) {
    return Registry.backedBy(new InjectorRegistryBacking(injector));
  }

  public static Function<Registry, Registry> registry(Action<? super BindingsSpec> bindings) {
    return baseRegistry -> registry(bindings, baseRegistry, newInjectorFactory(baseRegistry.get(ServerConfig.class)));
  }

  public static Function<Registry, Registry> registry(Injector parentInjector, Action<? super BindingsSpec> bindings) {
    return baseRegistry -> registry(bindings, baseRegistry, childInjectorFactory(parentInjector));
  }

  private static Registry registry(Action<? super BindingsSpec> bindings, Registry baseRegistry, Function<Module, Injector> injectorFactory) throws Exception {
    Injector injector = buildInjector(baseRegistry, bindings, injectorFactory);
    return registry(injector);
  }

  public static Function<Module, Injector> newInjectorFactory(final ServerConfig serverConfig) {
    final Stage stage = serverConfig.isDevelopment() ? Stage.DEVELOPMENT : Stage.PRODUCTION;
    return from -> from == null ? createInjector(stage) : createInjector(stage, from);
  }

  private static Function<Module, Injector> childInjectorFactory(final Injector parent) {
    return from -> from == null ? parent.createChildInjector() : parent.createChildInjector(from);
  }

  static Injector buildInjector(Registry baseRegistry, Action<? super BindingsSpec> bindingsAction, Function<? super Module, ? extends Injector> injectorFactory) throws Exception {
    List<Action<? super Binder>> binderActions = Lists.newLinkedList();
    List<Module> modules = Lists.newLinkedList();

    ServerConfig serverConfig = baseRegistry.get(ServerConfig.class);
    BindingsSpec bindings = new DefaultBindingsSpec(serverConfig, binderActions, modules);

    modules.add(new RatpackBaseRegistryModule(baseRegistry));
    modules.add(new ConfigModule(serverConfig.getRequiredConfig()));

    try {
      bindingsAction.execute(bindings);
    } catch (Exception e) {
      throw uncheck(e);
    }

    modules.add(new AdHocModule(binderActions));

    Optional<BindingsImposition> bindingsImposition = Impositions.current().get(BindingsImposition.class);
    if (bindingsImposition.isPresent()) {
      BindingsImposition imposition = bindingsImposition.get();
      List<Action<? super Binder>> imposedBinderActions = Lists.newLinkedList();
      List<Module> imposedModules = Lists.newLinkedList();

      BindingsSpec imposedBindings = new DefaultBindingsSpec(serverConfig, imposedBinderActions, imposedModules);
      imposition.getBindings().execute(imposedBindings);
      imposedModules.add(new AdHocModule(imposedBinderActions));
      Module imposedModule = imposedModules.stream().reduce((acc, next) -> Modules.override(acc).with(next)).get();

      modules.add(imposedModule);
    }

    Module masterModule = modules.stream().reduce((acc, next) -> Modules.override(acc).with(next)).get();

    return injectorFactory.apply(masterModule);
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

  private static class ConfigModule implements Module {
    private final Iterable<ConfigObject<?>> config;

    public ConfigModule(Iterable<ConfigObject<?>> config) {
      this.config = config;
    }

    @Override
    public void configure(Binder binder) {

      for (ConfigObject<?> configObject : config) {
        bind(binder, configObject);
      }
    }

    private static <T> void bind(Binder binder, ConfigObject<T> configObject) {
      binder.bind(GuiceUtil.toTypeLiteral(configObject.getTypeToken())).toInstance(configObject.getObject());
    }
  }
}
