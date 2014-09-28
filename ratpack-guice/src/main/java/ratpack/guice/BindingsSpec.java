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

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import ratpack.func.Action;
import ratpack.launch.LaunchConfig;

import javax.inject.Provider;
import java.util.function.Consumer;

/**
 * A buildable specification of Guice bindings.
 * <p>
 * This type is used when bootstrapping a Guice based application to add {@link Module modules} and bindings.
 * <p>
 * It is recommended to become familiar with Guice bindings, scopes and other concerns before using Guice with Ratpack.
 *
 * <pre class="tested">
 * import ratpack.guice.*;
 * import ratpack.func.Action;
 * import com.google.inject.AbstractModule;
 *
 * class MyService {
 *   private final String value;
 *   public MyService(String value) {
 *     this.value = value;
 *   }
 * }
 *
 * class MyModule extends AbstractModule {
 *   public String serviceValue;
 *
 *   protected void configure() {
 *     bind(MyService.class).toInstance(new MyService(serviceValue));
 *   }
 * }
 *
 * class ModuleAction implements Action&lt;BindingsSpec&gt; {
 *   public void execute(BindingsSpec bindings) {
 *     // MyModule has been added by some other action that executed against this registry…
 *
 *     bindings.config(MyModule.class) { it.serviceValue = "foo" };
 *   }
 * }
 * </pre>
 *
 * <h3>Module order and overrides</h3>
 * <p>
 * The order in which modules are added is significant.
 * Subsequent modules can <b>override</b> the bindings of previous modules.
 * This is a very useful technique for augmenting/customising the functionality provided by modules.
 * Many modules provide extensive bindings to facilitate such overriding.
 * <p>
 * Bindings added via the {@code bind()} and {@code provider()} methods always have the highest precedence, regardless of order.
 * That is, non module bindings can always override module bindings regardless of whether the module is added before or after the non module binding.
 * <h3>Adding handlers</h3>
 * <p>
 * Added modules can implement the {@link HandlerDecoratingModule} interface to facilitate adding handlers implicitly to the handler chain.
 *
 * @see Guice#builder(ratpack.launch.LaunchConfig)
 * @see HandlerDecoratingModule
 */
public interface BindingsSpec {

  /**
   * The launch config for the application.
   *
   * @return the launch config for the application
   */
  LaunchConfig getLaunchConfig();

  /**
   * Adds the bindings from the given modules.
   *
   * @param modules modules whose bindings should be added
   * @return this
   */
  BindingsSpec add(Module... modules);

  /**
   * Adds the bindings from the given modules.
   *
   * @param modules modules whose bindings should be added
   * @return this
   */
  BindingsSpec add(Iterable<? extends Module> modules);

  /**
   * Retrieves the module that has been added with the given type for configuration.
   * <p>
   * This can be used to configure modules that have already been added by some other mechanism.
   *
   * @param moduleClass the type of the module to retrieve
   * @param configurer the configurer of the module
   * @param <T> the type of the module to retrieve
   * @return this
   * @throws NoSuchModuleException if no module has been added with the given type
   */
  <T extends Module> BindingsSpec config(Class<T> moduleClass, Consumer<? super T> configurer) throws NoSuchModuleException;

  /**
   * Adds bindings by directly configuring a {@link Binder}.
   *
   * @param action the binder configuration
   * @return this
   */
  BindingsSpec bindings(Action<? super Binder> action);

  /**
   * Add a binding for the given type.
   *
   * @param type the type to add a binding for
   * @return this
   */
  BindingsSpec bind(Class<?> type);

  /**
   * Add a binding for the given public type, to the given implementation type.
   *
   * @param publicType the public type of the binding
   * @param implType the class implementing the public type
   * @param <T> the public type of the binding
   * @return this
   */
  <T> BindingsSpec bind(Class<T> publicType, Class<? extends T> implType);

  /**
   * Add a binding for the given public type, to the given implementing instance.
   *
   * @param publicType the public type of the binding
   * @param instance the instance that implements the public type
   * @param <T> the public type of the binding
   * @return this
   */
  <T> BindingsSpec bind(Class<? super T> publicType, T instance);

  /**
   * Add a binding for the given object to its concrete type.
   *
   * @param instance the instance to bind
   * @param <T> the type of the binding
   * @return this
   */
  <T> BindingsSpec bind(T instance);

  /**
   * Add a binding for the given public type, to the given provider type.
   *
   * @param publicType the public type of the object
   * @param providerType the type of the provider for the object
   * @param <T> The public type of the object
   * @return this
   */
  <T> BindingsSpec provider(Class<T> publicType, Class<? extends Provider<? extends T>> providerType);

  /**
   * Registers an action to operate on the injector when it has been finalized.
   * <p>
   * This can be used to do post processing of registered objects or application initialisation.
   *
   * @param action the action to execute against the constructed injector
   * @return this
   */
  BindingsSpec init(Action<Injector> action);

  /**
   * Registers a runnable to instantiated via dependency injection when the injector is created from this module registry.
   * <p>
   * This facilitates writing a {@link Runnable} implementation that uses constructor injection to get hold of what it needs to for the initialization.
   *
   * @param clazz the class of the runnable to execute as an init action
   * @return this
   */
  BindingsSpec init(Class<? extends Runnable> clazz);

}
