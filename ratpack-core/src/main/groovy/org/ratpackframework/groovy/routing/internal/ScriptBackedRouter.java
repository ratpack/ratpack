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

package org.ratpackframework.groovy.routing.internal;

import com.google.inject.Injector;
import org.ratpackframework.Handler;
import org.ratpackframework.config.LayoutConfig;
import org.ratpackframework.config.RoutingConfig;
import org.ratpackframework.groovy.ScriptEngine;
import org.ratpackframework.handler.HttpExchange;
import org.ratpackframework.handler.internal.CompositeRoutingHandler;
import org.ratpackframework.routing.ResponseFactory;
import org.ratpackframework.routing.Routed;
import org.ratpackframework.routing.internal.ReloadableFileBackedHandler;
import org.ratpackframework.routing.internal.RoutingBuilder;

import javax.inject.Inject;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

public class ScriptBackedRouter implements Handler<Routed<HttpExchange>> {

  private final Injector injector;
  private final ResponseFactory responseFactory;
  private final Handler<Routed<HttpExchange>> reloadHandler;
  private final File file;
  private final boolean staticallyCompile;

  @Inject
  public ScriptBackedRouter(final Injector injector, final ResponseFactory responseFactory, LayoutConfig layoutConfig, RoutingConfig routingConfig) {
    this.injector = injector;
    file = new File(layoutConfig.getBaseDir(), routingConfig.getFile());
    this.responseFactory = responseFactory;

    staticallyCompile = routingConfig.isStaticallyCompile();
    boolean reloadable = routingConfig.isReloadable();

    this.reloadHandler = new ReloadableFileBackedHandler<>(file, reloadable, new ReloadableFileBackedHandler.Delegate<HttpExchange>() {
      @Override
      public Handler<Routed<HttpExchange>> produce(File file, byte[] bytes) {
        List<Handler<Routed<HttpExchange>>> routers = new LinkedList<>();
        RoutingBuilder routingBuilder = new RoutingBuilder(injector, routers, responseFactory);
        String string;
        try {
          string = new String(bytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
        ScriptEngine<RoutingBuilderScript> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), staticallyCompile, RoutingBuilderScript.class);
        try {
          scriptEngine.run(file.getName(), string, routingBuilder);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }

        return new CompositeRoutingHandler<HttpExchange>(routers);
      }
    });
  }

  @Override
  public void handle(final Routed<HttpExchange> routedHttpExchange) {
    reloadHandler.handle(routedHttpExchange);
  }

}
