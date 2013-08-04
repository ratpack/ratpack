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

import com.google.inject.Module;

import java.util.List;

/**
 * A container of modules, used for specifying which modules to back an application with.
 * <p>
 * Ratpack adds the special {@link HandlerDecoratingModule} interface that modules can implement if they want
 * to influence the handler “chain”.
 * <p>
 * The order that modules are registered in is important.
 * Modules registered later can override the bindings of modules registered prior.
 * This can be useful for overriding default implementation bindings.
 *
 * @see Guice#handler(org.ratpackframework.launch.LaunchConfig, org.ratpackframework.util.Action, org.ratpackframework.handling.Handler)
 * @see HandlerDecoratingModule
 */
public interface ModuleRegistry {

  /**
   * Registers the given module.
   *
   * @param module The module to register.
   */
  void register(Module module);

  /**
   * Can be used to retrieve a registered module by type.
   * <p>
   * Some modules have properties/methods that can be used to configure their bindings.
   * This method can be used to retrieve such modules and configure them.
   * The modules have have already been registered “upstream”.
   * <pre class="tested">
   * import org.ratpackframework.guice.*;
   * import org.ratpackframework.util.*;
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
   * @param moduleType The type of the module to retrieve
   * @param <T> The module type
   * @throws NoSuchModuleException If there is no module of this exact type registered
   * @return The registered module of the given type
   */
  <T extends Module> T get(Class<T> moduleType) throws NoSuchModuleException;

  /**
   * Removes the module of the given type from the registry.
   *
   * @param moduleType The type of the module to remove
   * @throws NoSuchModuleException if there is no module of this exact type registered
   * @return The removed module
   */
  <T extends Module> T remove(Class<T> moduleType) throws NoSuchModuleException;

  /**
   * All of the currently registered modules.
   * <p>
   * The returned list is the actual list of modules in order that they will be used. It is mutable.
   *
   * @return The actual registered modules, in registration order
   */
  List<? extends Module> getModules();

}
