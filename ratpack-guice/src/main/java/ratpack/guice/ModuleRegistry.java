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

import com.google.inject.Module;
import ratpack.launch.LaunchConfig;
import ratpack.registry.MutableRegistry;

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
 * import ratpack.util.*;
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
 * @see Guice#handler(ratpack.launch.LaunchConfig, ratpack.util.Action, ratpack.util.Action)
 * @see HandlerDecoratingModule
 */
public interface ModuleRegistry extends MutableRegistry<Module> {

  /**
   * The launch config for the application the module registry is for.
   *
   * @return the launch config for the application the module registry is for.
   */
  LaunchConfig getLaunchConfig();

  void bind(Class<?> type);

  <T> void bind(Class<T> publicType, Class<? extends T> implType);

  <T> void bind(Class<? super T> publicType, T instance);

  <T> void bind(T instance);

  <T> void provider(Class<T> publicType, Class<? extends Provider<? extends T>> providerType);

}
