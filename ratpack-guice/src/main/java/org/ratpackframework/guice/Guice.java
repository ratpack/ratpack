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

import static com.google.inject.Guice.createInjector;
import static org.ratpackframework.handling.Handlers.chain;

/**
 * Static utility methods for creating Google Guice based Ratpack infrastructure.
 * <p>
 * Any non trivial application will require supporting objects for the handler implementations, for persistence for example.
 * These supporting objects can be managed by Guice and made available to handlers (either by dependency injection or registry lookup)
 * for increased reusability, modularity and testability.
 * <p>
 * <p>
 * The Guice integration is not part of the Ratpack core library, but is available as a separate add on.
 * That said, Ratpack is designed to support something like Guice as an integral piece via it's {@link Registry} abstraction.
 * Guice is the “official” solution.
 * </p>
 * <h4>Starting a Guice Ratpack app</h4>
 * <p>
 * The user entry point for Ratpack applications is the {@link org.ratpackframework.launch.HandlerFactory} implementation
 * that is given to the {@link org.ratpackframework.launch.LaunchConfigBuilder#build(org.ratpackframework.launch.HandlerFactory)} method.
 * This implementation can use one of the {@code handler(...) } static methods.
 * <p>
 * Below is a complete example for bootstrapping a Guice based Ratpack application.
 * </p>
 * <pre class="tested">
 * import org.ratpackframework.handling.*;
 * import org.ratpackframework.registry.*;
 * import org.ratpackframework.guice.*;
 * import org.ratpackframework.launch.*;
 * import org.ratpackframework.util.*;
 * import com.google.inject.*;
 * import javax.inject.*;
 *
 * // Some service to be dependency injected
 * class SomeService {}
 *
 * // A Guice module that provides the service
 * class ServiceModule extends AbstractModule {
 * protected void configure() {
 * bind(SomeService.class);
 * }
 * }
 *
 * // An action that registers the module with the registry, making it part of the application
 * class ModuleBootstrap implements Action&lt;ModuleRegistry&gt; {
 * public void execute(ModuleRegistry modules) {
 * modules.register(new ServiceModule());
 * }
 * }
 *
 * // A handler implementation that is dependency injected
 * {@literal @}Singleton
 * class InjectedHandler implements Handler {
 * private final SomeService service;
 *
 * {@literal @}Inject
 * public InjectedHandler(SomeService service) {
 * this.service = service;
 * }
 *
 * public void handle(Context exchange) {
 * // …
 * }
 * }
 *
 * // A chain configurer that adds a dependency injected handler to the chain
 * class HandlersBootstrap implements Action&lt;Chain&gt; {
 * public void execute(Chain chain) {
 *
 * // The registry in a Guice backed chain can be used to retrieve objects that were bound,
 * // or to create objects that are bound “just-in-time”.
 * Registry&lt;Object&gt; registry = chain.getRegistry();
 * Handler injectedHandler = registry.get(InjectedHandler.class);
 *
 * // Add the handler into the chain
 * chain.get("some/path", injectedHandler);
 * }
 * }
 *
 * // A HandlerFactory implementation used to bootstrap the application
 * class MyHandlerFactory implements HandlerFactory {
 * public Handler create(LaunchConfig launchConfig) {
 * return Guice.handler(launchConfig, new ModuleBootstrap(), new HandlersBootstrap());
 * }
 * }
 *
 * // Building a launch config with our handler factory
 * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("appRoot")).build(new MyHandlerFactory());
 * </pre>
 * <h4>Accessing Guice bound objects in Handlers</h4>
 * <p>
 * There are two ways to use Guice bound objects in your handler implementations.
 * </p>
 * <h5>Dependency Injected Handlers</h5>
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
 * <h5>Accessing dependencies via context registry lookup</h5>
 * <p>
 * The {@link org.ratpackframework.handling.Context} object that is given to a handler's {@link Handler#handle(org.ratpackframework.handling.Context)}
 * method is also a registry implementation. In a Guice backed app, Guice bound objects can be retrieved via the {@link Registry#get(Class)} method of
 * the context. You can retrieve any bound object via its publicly bound type.
 * </p>
 * <p>
 * However, this will not create “just-in-time” bindings. Only objects that were explicitly bound can be retrieved this way.
 * </p>
 * <h4>Guice modules as Ratpack “plugins”.</h4>
 * <p>
 * Add on Ratpack functionality is typically provided via Guice modules.
 * For example, the <a href="https://github.com/FasterXML/jackson-databind">Jackson</a> integration for JSON serialisation
 * is provided by the {@code ratpack-jackson} add-on which ships a Guice module.
 * To use its functionality simply register the module it provides with the {@link ModuleRegistry} used to bootstrap the application.
 * </p>
 * <h4>Groovy Applications</h4>
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
   * Creates a handler that can be used as the entry point for a Guice backed Ratpack app.
   * <p>
   * Modules are processed eagerly. Before this method returns, the modules will be used to create a single {@link com.google.inject.Injector} instance.
   * This injector is available to the given handler via service lookup.
   *
   * @param launchConfig The launch config of the server
   * @param moduleConfigurer The configurer of the {@link ModuleRegistry} to back the created handler
   * @param chainConfigurer The configurer that builds the handler chain
   * @return A handler that makes all Guice bound objects available to the handlers added to the {@link Chain} given to {@code chainConfigurer}.
   */
  public static Handler handler(LaunchConfig launchConfig, Action<? super ModuleRegistry> moduleConfigurer, final Action<? super Chain> chainConfigurer) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig).create(moduleConfigurer, newInjectorFactory(), new InjectorHandlerTransformer(launchConfig, chainConfigurer));
  }

  /**
   * Creates a handler that can be used as the entry point for a Guice backed Ratpack app.
   * <p>
   * This is a lower level version of {@link #handler(org.ratpackframework.launch.LaunchConfig, org.ratpackframework.util.Action, org.ratpackframework.util.Action)}
   * that supports a custom final {@link Handler} creation strategy.
   *
   * @param launchConfig The launch config of the server
   * @param moduleConfigurer The configurer of the {@link ModuleRegistry} to back the created handler
   * @param injectorTransformer Takes the final {@link Injector} instance that was created from the modules and returns the {@link Handler} to use
   * @return A handler that makes all Guice bound objects available to the handlers added to the {@link Chain} given to {@code chainConfigurer}.
   */
  public static Handler handler(LaunchConfig launchConfig, Action<? super ModuleRegistry> moduleConfigurer, Transformer<? super Injector, ? extends Handler> injectorTransformer) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig).create(moduleConfigurer, newInjectorFactory(), injectorTransformer);
  }

  /**
   * Creates a handler that can be used as the entry point for a Guice backed Ratpack app.
   * <p>
   * Similar to {@link #handler(org.ratpackframework.launch.LaunchConfig, org.ratpackframework.util.Action, org.ratpackframework.util.Action)},
   * but provides the opportunity to use a <i>parent</i> injector when creating the injector to use for the application.
   * See Guice documentation for the semantics of parent/child injectors.
   * <p>
   * Typically only required when making a Ratpack application a component of a larger application.
   *
   * @param launchConfig The launch config of the server
   * @param parentInjector The injector to use as a parent of the injector to be created
   * @param moduleConfigurer The configurer of the {@link ModuleRegistry} to back the created handler
   * @param chainConfigurer The configurer that builds the handler chain
   * @return A handler that makes all Guice bound objects available to the handlers added to the {@link Chain} given to {@code chainConfigurer}.
   */
  public static Handler handler(LaunchConfig launchConfig, Injector parentInjector, Action<? super ModuleRegistry> moduleConfigurer, final Action<? super Chain> chainConfigurer) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig).create(moduleConfigurer, childInjectorFactory(parentInjector), new InjectorHandlerTransformer(launchConfig, chainConfigurer));
  }

  /**
   * Creates a handler that can be used as the entry point for a Guice backed Ratpack app.
   * <p>
   * This is a lower level version of {@link #handler(org.ratpackframework.launch.LaunchConfig, com.google.inject.Injector, org.ratpackframework.util.Action, org.ratpackframework.util.Action)}
   * that supports a custom final {@link Handler} creation strategy.
   *
   * @param launchConfig The launch config of the server
   * @param parentInjector The injector to use as a parent of the injector to be created
   * @param moduleConfigurer The configurer of the {@link ModuleRegistry} to back the created handler
   * @param injectorTransformer Takes the final {@link Injector} instance that was created from the modules and returns the {@link Handler} to use
   * @return A handler that makes all Guice bound objects available to the handlers added to the {@link Chain} given to {@code chainConfigurer}.
   */
  public static Handler handler(LaunchConfig launchConfig, Injector parentInjector, Action<? super ModuleRegistry> moduleConfigurer, Transformer<? super Injector, ? extends Handler> injectorTransformer) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig).create(moduleConfigurer, childInjectorFactory(parentInjector), injectorTransformer);
  }

  /**
   * Creates a Ratpack {@link Registry} backed by the given {@link Injector} that will create objects via “just-in-time” binding.
   * <p>
   * Typically used in conjuction with the {@link org.ratpackframework.handling.Handlers#chain(LaunchConfig, org.ratpackframework.registry.Registry, org.ratpackframework.util.Action)}
   * method.
   *
   * @param injector The injector to back the registry
   * @return A registry that wraps the injector
   */
  public static Registry<Object> justInTimeRegistry(Injector injector) {
    return new JustInTimeInjectorRegistry(injector);
  }

  /**
   * Creates a Ratpack {@link Registry} backed by the given {@link Injector} that will create objects via “just-in-time” binding.
   * <p>
   * Typically used in conjuction with the {@link org.ratpackframework.handling.Handlers#chain(LaunchConfig, org.ratpackframework.registry.Registry, org.ratpackframework.util.Action)}
   * method.
   * <p>
   * If the injector cannot provide an instance of the requested type, the returned registry will delegate to the given parent.
   *
   * @param parent The parent registry to delegate to if the injector can't satisfy the registry {@link Registry#get(Class)} request.
   * @param injector The injector to back the registry
   * @return A registry that wraps the injector
   */
  public static Registry<Object> justInTimeRegistry(Registry<Object> parent, Injector injector) {
    return new JustInTimeInjectorChildRegistry(parent, injector);
  }

  /**
   * Creates a Ratpack {@link Registry} backed by the given {@link Injector} that will <b>NOT</b> create objects via “just-in-time” binding.
   * <p>
   * If the injector cannot provide an instance of the requested type (because no object of that type was bound),
   * the returned registry will delegate to the given parent.
   *
   * @param parent The parent registry to delegate to if the injector can't satisfy the registry {@link Registry#get(Class)} request.
   * @param injector The injector to back the registry
   * @return A registry that wraps the injector
   */
  public static Registry<Object> registry(Registry<Object> parent, Injector injector) {
    return new InjectorBackedChildRegistry(parent, injector);
  }

  /**
   * Creates a transformer that can build an injector from a module.
   * <p>
   * The module given to the {@code transform()} method may be {@code null}.
   *
   * @return a transformer that can build an injector from a module
   */
  public static Transformer<Module, Injector> newInjectorFactory() {
    return new Transformer<Module, Injector>() {
      @Override
      public Injector transform(Module from) {
        return from == null ? createInjector() : createInjector(from);
      }
    };
  }

  /**
   * Creates a transformer that can build an injector from a module, as a child of the given parent.
   * <p>
   * The module given to the {@code transform()} method may be {@code null}.
   *
   * @return a transformer that can build an injector from a module, as a child of the given parent.
   */
  public static Transformer<Module, Injector> childInjectorFactory(final Injector parent) {
    return new Transformer<Module, Injector>() {
      @Override
      public Injector transform(Module from) {
        return from == null ? parent.createChildInjector() : parent.createChildInjector(from);
      }
    };
  }

  private static class InjectorHandlerTransformer implements Transformer<Injector, Handler> {
    private final LaunchConfig launchConfig;
    private final Action<? super Chain> action;

    public InjectorHandlerTransformer(LaunchConfig launchConfig, Action<? super Chain> action) {
      this.launchConfig = launchConfig;
      this.action = action;
    }

    public Handler transform(Injector injector) {
      return chain(launchConfig, justInTimeRegistry(injector), action);
    }
  }
}
