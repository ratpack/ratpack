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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StandaloneScriptBacking implements Action<Closure<?>> {

  private final Lock lock = new ReentrantLock();
  private RatpackServer running;

  public static final StandaloneScriptBacking INSTANCE = new StandaloneScriptBacking();

  private StandaloneScriptBacking() {
  }

  public void execute(final Closure<?> closure) throws Exception {
    GroovyVersionCheck.ensureRequiredVersionUsed(GroovySystem.getVersion());

    lock.lock();
    try {
      if (running != null) {
        running.stop();
        running = null;
      }

      Path scriptFile = ClosureUtil.findScript(closure);
      if (scriptFile == null) {
        running = RatpackServer.start(new RatpackClosureConfigurer(closure, false));
      } else {
        running = RatpackServer.start(Groovy.Script.app(scriptFile));
      }
    } finally {
      lock.unlock();
    }
  }
}
