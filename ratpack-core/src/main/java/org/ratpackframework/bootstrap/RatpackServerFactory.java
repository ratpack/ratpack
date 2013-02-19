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
import org.ratpackframework.app.AppModule;
import org.ratpackframework.app.Routing;
import org.ratpackframework.app.internal.AppRoutingModule;
import org.ratpackframework.assets.StaticAssetsConfig;
import org.ratpackframework.assets.StaticAssetsModule;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.session.SessionModule;

import java.io.File;

import static com.google.inject.util.Modules.combine;
import static com.google.inject.util.Modules.override;

public class RatpackServerFactory {

  private final File baseDir;
  private final int bindPort;
  private final String bindHost;
  private final String publicHost;

  public RatpackServerFactory(File baseDir, int bindPort, String bindHost, String publicHost) {
    this.baseDir = baseDir;
    this.bindPort = bindPort;
    this.bindHost = bindHost;
    this.publicHost = publicHost;
  }

  public RatpackServer create(Handler<Routing> appHandler, Module... modules) {
    return create(AppRoutingModule.create(appHandler), null, modules);
  }

  public RatpackServer create(Handler<Routing> appHandler, File staticAssetsDir, Module... modules) {
    return create(AppRoutingModule.create(appHandler), new StaticAssetsConfig(staticAssetsDir), modules);
  }

  public RatpackServer create(Handler<Routing> appHandler, StaticAssetsConfig staticAssetsConfig, Module... modules) {
    return create(AppRoutingModule.create(appHandler), staticAssetsConfig, modules);
  }

  public RatpackServer create(Class<? extends Handler<Routing>> appHandler, Module... modules) {
    return create(AppRoutingModule.create(appHandler), null, modules);
  }

  public RatpackServer create(Class<? extends Handler<Routing>> appHandler, File staticAssetsDir, Module... modules) {
    return create(AppRoutingModule.create(appHandler), new StaticAssetsConfig(staticAssetsDir), modules);
  }

  public RatpackServer create(Class<? extends Handler<Routing>> appHandler, StaticAssetsConfig staticAssetsConfig, Module... modules) {
    return create(AppRoutingModule.create(appHandler), staticAssetsConfig, modules);
  }

  public RatpackServer create(Module routingModule, StaticAssetsConfig staticAssetsConfig, Module... modules) {
    Module baseModule = routingModule;
    if (staticAssetsConfig != null) {
      baseModule = combine(routingModule, toModule(staticAssetsConfig));
    }

    return create(override(baseModule).with(modules));
  }

  private StaticAssetsModule toModule(StaticAssetsConfig staticAssetsConfig) {
    return new StaticAssetsModule(staticAssetsConfig.getDirectory(), staticAssetsConfig.getIndexFiles());
  }

  public RatpackServer create(Module... overrideModules) {
    Module finalModule = override(createRootModule()).with(overrideModules);
    Injector injector = Guice.createInjector(finalModule);
    return injector.getInstance(RatpackServer.class);
  }

  protected Module createRootModule() {
    return combine(new RootModule(baseDir, bindPort, bindHost, publicHost), new AppModule(), new SessionModule());
  }

}
