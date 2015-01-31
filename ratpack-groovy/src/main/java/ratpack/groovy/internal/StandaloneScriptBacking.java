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
import ratpack.server.RatpackServer;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class StandaloneScriptBacking implements Action<Closure<?>> {

  private final static AtomicReference<Action<? super RatpackServer>> CAPTURE_ACTION = new AtomicReference<>(null);

  public static void captureNext(Action<? super RatpackServer> action) {
    CAPTURE_ACTION.set(action);
  }

  public void execute(final Closure<?> closure) throws Exception {
    GroovyVersionCheck.ensureRequiredVersionUsed(GroovySystem.getVersion());
    Path scriptFile = ClosureUtil.findScript(closure);
    if (scriptFile == null) {
      throw new IllegalStateException("Could not find script file for closure: " + closure.toString());
    }

    RatpackServer server = RatpackServer.of(Groovy.Script.app(scriptFile));

    Action<? super RatpackServer> action = CAPTURE_ACTION.getAndSet(null);
    if (action != null) {
      action.execute(server);
    }

    server.start();
  }

}
