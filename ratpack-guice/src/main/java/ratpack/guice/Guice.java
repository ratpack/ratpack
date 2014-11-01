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

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.guice.internal.DefaultGuiceBackedHandlerFactory;
import ratpack.guice.internal.InjectorRegistryBacking;
import ratpack.guice.internal.JustInTimeInjectorRegistry;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registries;
import ratpack.registry.Registry;

import static com.google.inject.Guice.createInjector;

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
 * The user entry point for Ratpack applications is the {@link ratpack.launch.HandlerFactory} implementation
 * that is given to the {@link ratpack.launch.LaunchConfigBuilder#build(ratpack.launch.HandlerFactory)} method.
 * This implementation can use one of the {@code handler(...) } static methods.
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
 *   public static void main(String... args) {
 *     EmbeddedApp.fromHandlerFactory(launchConfig ->
 *         Guice.builder(launchConfig)
 *           .bindings(b -> b.add(new ServiceModule()))
 *           .build(chain -> {
 *             // The registry in a Guice backed chain can be used to retrieve objects that were bound,
 *             // or to create objects that are bound “just-in-time”.
 *             chain.get("some/path", InjectedHandler.class);
 *           })
 *     ).test(httpClient -> {
 *       assert httpClient.get("some/path").getBody().getText().equals("foo-bar");
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

  public static Builder builder(LaunchConfig launchConfig) {
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
        Function<Module, Injector> moduleTransformer = parent == null ? newInjectorFactory(launchConfig) : childInjectorFactory(parent);
        return new DefaultGuiceBackedHandlerFactory(launchConfig).create(bindings, moduleTransformer,
          injector -> Handlers.chain(launchConfig, justInTimeRegistry(injector), action)
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

  /**
   * Creates a transformer that can build an injector from a module.
   * <p>
   * The module given to the {@code transform()} method may be {@code null}.
   *
   * @param launchConfig The launch config of the server
   * @return a transformer that can build an injector from a module
   */
  public static Function<Module, Injector> newInjectorFactory(final LaunchConfig launchConfig) {
    final Stage stage = launchConfig.isDevelopment() ? Stage.DEVELOPMENT : Stage.PRODUCTION;
    return from -> from == null ? createInjector(stage) : createInjector(stage, from);
  }

  private static Function<Module, Injector> childInjectorFactory(final Injector parent) {
    return from -> from == null ? parent.createChildInjector() : parent.createChildInjector(from);
  }

}
