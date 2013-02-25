/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.bootstrap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.ratpackframework.app.internal.AppModule;
import org.ratpackframework.app.Routing;
import org.ratpackframework.app.internal.AppRoutingModule;
import org.ratpackframework.assets.StaticAssetsConfig;
import org.ratpackframework.assets.internal.StaticAssetsModule;
import org.ratpackframework.bootstrap.internal.RootModule;
import org.ratpackframework.config.AddressConfig;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.session.SessionModule;

import java.io.File;

import static com.google.inject.util.Modules.combine;
import static com.google.inject.util.Modules.override;

/**
 * Factory for bootstrapping an application.
 */
public class RatpackServerFactory {

  private final int bindPort;
  private final String bindHost;
  private final String publicHost;

  /**
   * Expands the config to {@link #RatpackServerFactory(int, String, String)}.
   */
  public RatpackServerFactory(AddressConfig addressConfig) {
    this(addressConfig.getPort(), addressConfig.getBindHost(), addressConfig.getPublicHost());
  }

  /**
   * Creates a factory with the specified address config.
   */
  public RatpackServerFactory(int bindPort, String bindHost, String publicHost) {
    this.bindPort = bindPort;
    this.bindHost = bindHost;
    this.publicHost = publicHost;
  }

  /**
   * Creates an app using the given handler to specify the routing.
   * 
   * No static assets will be served.
   * 
   * @param appHandler The handler that defines the routing to app end points. 
   * @param modules Extra modules of services to register (which can override core services).
   * @return A (not yet running) server instance.
   */
  public RatpackServer create(Handler<Routing> appHandler, Module... modules) {
    return create(AppRoutingModule.create(appHandler), null, modules);
  }

  /**
   * Creates an app using the given handler to specify the routing.
   *
   * @param appHandler The handler that defines the routing to app end points. 
   * @param staticAssetsDir The directory containing static assets to serve
   * @param modules Extra modules of services to register (which can override core services).
   * @return A (not yet running) server instance.
   */
  public RatpackServer create(Handler<Routing> appHandler, File staticAssetsDir, Module... modules) {
    return create(AppRoutingModule.create(appHandler), new StaticAssetsConfig(staticAssetsDir), modules);
  }

  /**
   * Creates an app using the given handler to specify the routing.
   *
   * @param appHandler The handler that defines the routing to app end points. 
   * @param staticAssetsConfig The configuration for the serving of static assets.
   * @param modules Extra modules of services to register (which can override core services).
   * @return A (not yet running) server instance.
   */
  public RatpackServer create(Handler<Routing> appHandler, StaticAssetsConfig staticAssetsConfig, Module... modules) {
    return create(AppRoutingModule.create(appHandler), staticAssetsConfig, modules);
  }

  /**
   * Creates an app using the given handler type (which will be dependency injected) to specify the routing.
   * 
   * @param appHandler The type of the handler that defines routing to endpoints.
   * @param modules Extra modules of services to register (which can override core services).
   * @return A server instance
   */
  public RatpackServer create(Class<? extends Handler<Routing>> appHandler, Module... modules) {
    return create(AppRoutingModule.create(appHandler), null, modules);
  }

  /**
   * Creates an app using the given handler type (which will be dependency injected) to specify the routing.
   *
   * @param appHandler The type of the handler that defines routing to endpoints.
   * @param staticAssetsDir The directory containing static assets to serve
   * @param modules Extra modules of services to register (which can override core services).
   * @return A (not yet running) server instance.
   */
  public RatpackServer create(Class<? extends Handler<Routing>> appHandler, File staticAssetsDir, Module... modules) {
    return create(AppRoutingModule.create(appHandler), new StaticAssetsConfig(staticAssetsDir), modules);
  }

  /**
   * Creates an app using the given handler type (which will be dependency injected) to specify the routing.
   *
   * @param appHandler The type of the handler that defines routing to endpoints.
   * @param staticAssetsConfig The configuration for the serving of static assets.
   * @param modules Extra modules of services to register (which can override core services).
   * @return A (not yet running) server instance.
   */
  public RatpackServer create(Class<? extends Handler<Routing>> appHandler, StaticAssetsConfig staticAssetsConfig, Module... modules) {
    return create(AppRoutingModule.create(appHandler), staticAssetsConfig, modules);
  }

  /**
   * Creates an app allowing the app routing to be specified by a module.
   * 
   * This is a low level hook for deep customization of how the Ratpack app is put together.
   *
   * @param routingModule A module that defines the main routing handler.
   * @param staticAssetsConfig The configuration for the serving of static assets.
   * @param modules Extra modules of services to register (which can override core services).
   * @return A (not yet running) server instance.
   */
  public RatpackServer create(Module routingModule, StaticAssetsConfig staticAssetsConfig, Module... modules) {
    Module baseModule = routingModule;
    if (staticAssetsConfig != null) {
      baseModule = combine(routingModule, toModule(staticAssetsConfig));
    }

    return create(override(baseModule).with(modules));
  }

  /**
   * Creates an app using the given modules to define the structure of the application.
   * 
   * This is a low level hook for deep customization of how the Ratpack app is put together.
   * 
   * @param overrideModules Modules that specify the application (which can override core services)
   * @return A (not yet running) server instance.
   */
  public RatpackServer create(Module... overrideModules) {
    Module finalModule = override(createRootModule()).with(overrideModules);
    Injector injector = Guice.createInjector(finalModule);
    return injector.getInstance(RatpackServer.class);
  }

  private StaticAssetsModule toModule(StaticAssetsConfig staticAssetsConfig) {
    return new StaticAssetsModule(staticAssetsConfig.getDirectory(), staticAssetsConfig.getIndexFiles());
  }

  protected Module createRootModule() {
    return combine(new RootModule(bindPort, bindHost, publicHost), new AppModule(), new SessionModule());
  }

}
