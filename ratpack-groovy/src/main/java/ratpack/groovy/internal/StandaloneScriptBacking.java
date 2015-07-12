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

package ratpack.groovy.internal;

import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import ratpack.func.Action;
import ratpack.groovy.Groovy;
import ratpack.guice.Guice;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;
import ratpack.util.Exceptions;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class StandaloneScriptBacking implements Action<Closure<?>> {

  private final static AtomicReference<Action<? super RatpackServer>> CAPTURE_ACTION = new AtomicReference<>(null);
  private final ThreadLocal<RatpackServer> running = new ThreadLocal<>();

  public static void captureNext(Action<? super RatpackServer> action) {
    CAPTURE_ACTION.set(action);
  }

  public void execute(final Closure<?> closure) throws Exception {
    GroovyVersionCheck.ensureRequiredVersionUsed(GroovySystem.getVersion());

    Optional.ofNullable(running.get()).ifPresent(s -> Exceptions.uncheck(s::stop));

    RatpackServer ratpackServer;
    Path scriptFile = ClosureUtil.findScript(closure);
    if (scriptFile == null) {
      ratpackServer = RatpackServer.of(server -> ClosureUtil.configureDelegateFirst(new RatpackBacking(server), closure));
    } else {
      ratpackServer = RatpackServer.of(Groovy.Script.app(scriptFile));
      Action<? super RatpackServer> action = CAPTURE_ACTION.getAndSet(null);
      if (action != null) {
        action.execute(ratpackServer);
      }
    }

    ratpackServer.start();
    running.set(ratpackServer);
  }

  private static class RatpackBacking implements Groovy.Ratpack {
    private final RatpackServerSpec server;

    public RatpackBacking(RatpackServerSpec server) {
      this.server = server;
    }

    @Override
    public void bindings(Closure<?> configurer) {
      server.registry(Guice.registry(ClosureUtil.delegatingAction(configurer)));
    }

    @Override
    public void handlers(Closure<?> configurer) {
      Exceptions.uncheck(() -> server.handlers(Groovy.chainAction(configurer)));
    }

    @Override
    public void serverConfig(Closure<?> configurer) {
      ServerConfigBuilder builder = ServerConfig.noBaseDir().development(true);
      ClosureUtil.configureDelegateFirst(builder, configurer);
      server.serverConfig(builder);
    }
  }
}
