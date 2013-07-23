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

import org.ratpackframework.guice.internal.DefaultGuiceBackedHandlerFactory;
import org.ratpackframework.guice.internal.InjectingHandler;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.util.Action;

/**
 * Utilities for creating Guice backed handlers.
 */
public abstract class Guice {

  private Guice() {}

  /**
   * Creates a dependency injected handler by fetching the contextual {@link com.google.inject.Injector} and creating an instance of the given type.
   * <p>
   * The handler instance is created by using the {@link com.google.inject.Injector#getInstance(Class)} method.
   * The injected instance is created lazily.
   * For each request, the {@link com.google.inject.Injector} is retrieved from the exchange service registry.
   * An instance of the given handler type is then retrieved from the exchange.
   * <p>
   * To avoid having a new handler instance created each exchange, you should annotate the handler type with {@link javax.inject.Singleton}.
   * <pre class="tested">
   * import org.ratpackframework.handling.*;
   * import org.ratpackframework.guice.Guice;
   * import javax.inject.Singleton;
   * import javax.inject.Inject;
   *
   * interface SomeService {}
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
   *   public void handle(Exchange exchange) {
   *     // …
   *   }
   * }
   *
   * Handler injected = Guice.handler(InjectedHandler.class);
   * </pre>
   * <p>
   * The handler returned from this method <b>is not</b> of the given type.
   * It is a kind of proxy that creates an instance of the specified type on demand.
   * <p>
   * If there is no contextual {@link com.google.inject.Injector} for the exchange, a {@link org.ratpackframework.service.NotInServiceRegistryException} will be thrown.
   * This means that it only makes sense to use an injected handler like this if the application is Guice backed.
   * Such as when using a {@link #handler(org.ratpackframework.util.Action, org.ratpackframework.handling.Handler)} handler as the root.
   *
   * @param handlerType The type of handler to create via dependency injection
   * @return A handler that delegates to a created-on-demand dependency injected instance of the given type
   */
  public static Handler handler(Class<? extends Handler> handlerType) {
    return new InjectingHandler(handlerType);
  }

  /**
   * Creates a handler that can be used as the entry point for a Guice backed Ratpack app.
   * <p>
   * Typically used as the “root” handler for an app.
   * <pre class="tested">
   * import org.ratpackframework.handling.*;
   * import org.ratpackframework.guice.*;
   * import org.ratpackframework.bootstrap.*;
   * import org.ratpackframework.util.Action;
   * import com.google.inject.AbstractModule;
   * import javax.inject.Singleton;
   * import javax.inject.Inject;
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
   *   public void handle(Exchange exchange) {
   *     // …
   *   }
   * }
   *
   * Handler appHandlers = Handlers.chain(new Action&lt;Chain&gt;() {
   *  public void execute(Chain chain) {
   *    chain.add(Guice.handler(InjectedHandler.class));
   *  }
   * });
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
   * Handler guiceBackedHandler = Guice.handler(new ModuleBootstrap(), appHandlers);
   *
   * RatpackServerBuilder serverBuilder = new RatpackServerBuilder(guiceBackedHandler, new File("appRoot"))
   * </pre>
   * <p>
   * Modules are processed eagerly. Before this method returns, the modules will be used to create a single
   * {@link com.google.inject.Injector} instance. This injector is available to the given handler via service lookup.
   * A new injector <b>is not</b> created for each exchange (as this would be unnecessary and expensive).
   * <p>
   * The injector also makes all of its objects available via service lookup.
   * This means that you can retrieve objects that were bound by modules in handlers via {@link org.ratpackframework.handling.Exchange#get(Class)}.
   * Objects are only retrievable via their public type.
   *
   * @param moduleConfigurer The configurer of the {@link ModuleRegistry} to back the created handler
   * @param handler The handler of the application
   * @return A handler that makes the injector and its content available to the given handler
   */
  public static Handler handler(Action<? super ModuleRegistry> moduleConfigurer, Handler handler) {
    return new DefaultGuiceBackedHandlerFactory().create(moduleConfigurer, handler);
  }

}
