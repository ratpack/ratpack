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

package org.ratpackframework.groovy.internal;

import groovy.lang.Closure;
import groovy.lang.Script;
import io.netty.buffer.ByteBuf;
import org.ratpackframework.Action;
import org.ratpackframework.groovy.Closures;
import org.ratpackframework.groovy.routing.Routing;
import org.ratpackframework.groovy.routing.internal.RoutingHandler;
import org.ratpackframework.groovy.script.ScriptEngine;
import org.ratpackframework.guice.GuiceBackedHandlerFactory;
import org.ratpackframework.guice.ModuleRegistry;
import org.ratpackframework.reload.internal.Factory;
import org.ratpackframework.reload.internal.ReloadableFileBackedFactory;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.util.IoUtils;

import javax.inject.Inject;
import java.io.File;

public class ScriptBackedApp implements Handler {

  private final Factory<Handler> reloadHandler;

  @Inject
  public ScriptBackedApp(File script, final GuiceBackedHandlerFactory appFactory, final boolean staticCompile, boolean reloadable) {
    this.reloadHandler = new ReloadableFileBackedFactory<>(script, reloadable, new ReloadableFileBackedFactory.Delegate<Handler>() {
      @Override
      public Handler produce(final File file, final ByteBuf bytes) {
        try {
          final String string;
          string = IoUtils.utf8String(bytes);
          final ScriptEngine<Script> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), staticCompile, Script.class);

          Runnable runScript = new Runnable() {
            @Override
            public void run() {
              try {
                scriptEngine.run(file.getName(), string);
              } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
              }
            }
          };

          final DefaultRatpack ratpack = new DefaultRatpack();
          Action<Closure<?>> backing = new Action<Closure<?>>() {
            @Override
            public void execute(Closure<?> configurer) {
              Closures.configure(ratpack, configurer);
            }
          };

          RatpackScriptBacking.withBacking(backing, runScript);

          Action<ModuleRegistry> modulesAction = Closures.action(ModuleRegistry.class, ratpack.getModulesConfigurer());
          Action<Routing> routingAction = Closures.action(Routing.class, ratpack.getRoutingConfigurer());

          RoutingHandler routingHandler = new RoutingHandler(routingAction);
          return appFactory.create(modulesAction, routingHandler);

        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @Override
  public void handle(Exchange exchange) {
    reloadHandler.create().handle(exchange);
  }

}
