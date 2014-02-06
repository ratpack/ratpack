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
import ratpack.launch.LaunchConfig;
import ratpack.registry.MutableRegistry;
import ratpack.func.Action;

import javax.inject.Provider;

/**
 * A container of modules, used for specifying which modules to back an application with.
 * <p>
 * Ratpack adds the special {@link HandlerDecoratingModule} interface that modules can implement if they want
 * to influence the handler “chain”.
 * <p>
 * The order that modules are registered in is important.
 * Modules registered later can override the bindings of modules registered prior.
 * This can be useful for overriding default implementation bindings.
 * <p>
 * Some modules have properties/methods that can be used to configure their bindings.
 * This method can be used to retrieve such modules and configure them.
 * The modules have have already been registered “upstream”.
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
 * class ModuleAction implements Action&lt;ModuleRegistry&gt; {
 *   public void execute(ModuleRegistry modules) {
 *     // MyModule has been added by some other action that executed against this registry…
 *
 *     modules.get(MyModule.class).serviceValue = "foo";
 *   }
 * }
 * </pre>
 *
 * @see Guice#handler(ratpack.launch.LaunchConfig, ratpack.func.Action, ratpack.func.Action)
 * @see HandlerDecoratingModule
 */
public interface ModuleRegistry extends MutableRegistry<Module> {

  /**
   * The launch config for the application the module registry is for.
   *
   * @return the launch config for the application the module registry is for.
   */
  LaunchConfig getLaunchConfig();

  /**
   * Bind the given type with Guice.
   * <p>
   * This can be used to add types to the application without writing a module.
   * <p>
   * If the type should be bound as a singleton, annotate it with {@link javax.inject.Singleton}.
   *
   * @param type The type to register with Guice.
   */
  void bind(Class<?> type);

  /**
   * Bind the given implementation type with Guice, under the given public type.
   * <p>
   * This can be used to add types to the application without writing a module.
   * <p>
   * If the type should be bound as a singleton, annotate it with {@link javax.inject.Singleton}.
   *
   * @param publicType The public type of the binding
   * @param implType The class implementing the public type
   * @param <T> The public type of the binding
   */
  <T> void bind(Class<T> publicType, Class<? extends T> implType);

  /**
   * Bind the given implementation with Guice, under the given public type.
   * <p>
   * This can be used to add types to the application without writing a module.
   * <p>
   * If the type should be bound as a singleton, annotate it with {@link javax.inject.Singleton}.
   *
   * @param publicType The public type of the binding
   * @param instance The instance to make available
   * @param <T> The public type of the binding
   */
  <T> void bind(Class<? super T> publicType, T instance);

  /**
   * Bind the given implementation with Guice, under its concrete type.
   * <p>
   * This can be used to add types to the application without writing a module.
   * <p>
   * If the type should be bound as a singleton, annotate it with {@link javax.inject.Singleton}.
   *
   * @param instance The instance to make available
   * @param <T> The type of the binding
   */
  <T> void bind(T instance);

  /**
   * Bind the given provider with Guice, under the given type.
   * <p>
   * This can be used to add types to the application without writing a module.
   *
   * @param publicType The public type of the object
   * @param providerType The type of the provider for the object
   * @param <T> The public type of the object
   */
  <T> void provider(Class<T> publicType, Class<? extends Provider<? extends T>> providerType);

  /**
   * Registers an action to operate on the injector when it has been finalized.
   * <p>
   * This can be used to do post processing of registered objects or application initialisation.
   *
   * @param action The action to execute against the constructed injector
   */
  void init(Action<Injector> action);

  /**
   * Registers a runnable to instantiated via dependency injection when the injector is created from this module registry.
   * <p>
   * This facilitates writing a {@link Runnable} implementation that uses constructor injection to get hold of what it needs to for the initialization.
   *
   * @param clazz The class of the runnable to execute as an init action
   */
  void init(Class<? extends Runnable> clazz);

}
