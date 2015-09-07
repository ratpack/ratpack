/*
 * Copyright 2015 the original author or authors.
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

package ratpack.gradle.continuous.run;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLClassLoader;

public class DefaultRatpackAdapter implements RatpackAdapter, Serializable {

  private static final Logger LOGGER = Logging.getLogger(RatpackAdapter.class);

  private final RatpackSpec spec;

  private boolean started;
  private ClassLoader baseLoader;
  private ClassLoader appLoader;
  private RatpackServerProxy server;

  public DefaultRatpackAdapter(RatpackSpec spec) {
    this.spec = spec;
  }

  @Override
  public void start() {
    if (started) {
      throw new IllegalStateException("already started");
    }
    started = true;
    baseLoader = new URLClassLoader(spec.getClasspath(), null);
    reload();
  }

  @Override
  public void reload() {
    stop();
    if (appLoader != null && appLoader instanceof Closeable) {
      try {
        ((Closeable) appLoader).close();
      } catch (IOException e) {
        LOGGER.warn("failed to close old classloader", e);
      }
    }

    appLoader = new URLClassLoader(spec.getChangingClasspath(), baseLoader);
    inContext(new Runnable() {
      @Override
      public void run() {
        server = RatpackServerProxy.capture(appLoader, spec.getMainClass(), spec.getArgs());
      }
    });
  }

  private void inContext(Runnable runnable) {
    Thread currentThread = Thread.currentThread();
    ClassLoader threadClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(appLoader);
    String threadName = currentThread.getName();
    currentThread.setName("main");
    try {
      runnable.run();
    } finally {
      currentThread.setContextClassLoader(threadClassLoader);
      currentThread.setName(threadName);
    }
  }

  @Override
  public void buildError(Throwable throwable) {
    // ignore for now
  }

  @Override
  public boolean isRunning() {
    return server != null && server.isRunning();
  }

  @Override
  public void stop() {
    if (server != null) {
      inContext(new Runnable() {
        @Override
        public void run() {
          server.stop();
        }
      });
    }
  }
}
