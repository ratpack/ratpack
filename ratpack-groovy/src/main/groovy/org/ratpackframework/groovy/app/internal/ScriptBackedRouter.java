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

package org.ratpackframework.groovy.app.internal;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ratpackframework.app.Routing;
import org.ratpackframework.app.internal.ReloadableFileBackedHandler;
import org.ratpackframework.app.internal.RoutingConverter;
import org.ratpackframework.groovy.ScriptEngine;
import org.ratpackframework.groovy.app.RoutingConfig;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;
import org.ratpackframework.util.IoUtils;

import javax.inject.Inject;
import java.io.File;

public class ScriptBackedRouter implements Handler<Routed<HttpExchange>> {

  private final Handler<Routed<HttpExchange>> reloadHandler;

  @Inject
  public ScriptBackedRouter(final RoutingConverter routingConverter, RoutingConfig routingConfig) {
    File file = routingConfig.getScript();

    final boolean staticallyCompile = routingConfig.isStaticallyCompile();
    boolean reloadable = routingConfig.isReloadable();

    this.reloadHandler = new ReloadableFileBackedHandler<>(file, reloadable, new ReloadableFileBackedHandler.Delegate<HttpExchange>() {
      @Override
      public Handler<Routed<HttpExchange>> produce(final File file, final ChannelBuffer bytes) {
        return routingConverter.build(new Handler<Routing>() {
          @Override
          public void handle(Routing event) {
            try {
              final String string;
              string = IoUtils.utf8String(bytes);
              final ScriptEngine<RoutingScript> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), staticallyCompile, RoutingScript.class);
              scriptEngine.run(file.getName(), string, event);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
    });
  }

  @Override
  public void handle(final Routed<HttpExchange> routedHttpExchange) {
    reloadHandler.handle(routedHttpExchange);
  }

}
