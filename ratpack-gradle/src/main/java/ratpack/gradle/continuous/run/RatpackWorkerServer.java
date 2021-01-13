/*
 * Copyright 2021 the original author or authors.
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

import org.gradle.api.Action;
import org.gradle.internal.UncheckedException;
import ratpack.gradle.internal.Invoker;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;

public class RatpackWorkerServer implements Action<Object>, RatpackAdapter, Serializable {
  public RatpackWorkerServer(RatpackAdapter runAdapter) {
    this.runAdapter = runAdapter;
  }

  public void execute(Object context) {
    disableUrlConnectionCaching();
    Object serverConnection = Invoker.invokeParamless(
      "org.gradle.process.internal.worker.WorkerProcessContext",
      context,
      "getServerConnection"
    );

    Invoker.of("org.gradle.internal.remote.ObjectConnection", "addIncoming", Class.class, Object.class)
      .invoke(serverConnection, RatpackAdapter.class, this);

    signal = (Signal) Invoker.of("org.gradle.internal.remote.ObjectConnection", "addOutgoing", Class.class)
      .invoke(serverConnection, Signal.class);

    Invoker.invokeParamless("org.gradle.internal.remote.ObjectConnection", serverConnection, "connect");
    latch = new CountDownLatch(1);
    try {
      latch.await();
    } catch (InterruptedException ignore) {
      // empty
    }
  }

  @Override
  public void start() {
    runAdapter.start();
    signal.fire();
  }

  @Override
  public void reload() {
    runAdapter.reload();
    signal.fire();
  }

  @Override
  public boolean isRunning() {
    return runAdapter.isRunning();
  }

  @Override
  public void stop() {
    latch.countDown();
    runAdapter.stop();
  }

  @Override
  public void buildError(Throwable throwable) {
    runAdapter.buildError(throwable);
    signal.fire();
  }

  private static void disableUrlConnectionCaching() {
    // fix problems in updating jar files by disabling default caching of URL connections.
    // URLConnection default caching should be disabled since it causes jar file locking issues and JVM crashes in updating jar files.
    // Changes to jar files won't be noticed in all cases when caching is enabled.
    // sun.net.www.protocol.jar.JarURLConnection leaves the JarFile instance open if URLConnection caching is enabled.
    try {
      URL url = new URL("jar:file://valid_jar_url_syntax.jar!/");
      URLConnection urlConnection = url.openConnection();
      urlConnection.setDefaultUseCaches(false);
    } catch (IOException e) {
      throw UncheckedException.throwAsUncheckedException(e);
    }
  }

  private final RatpackAdapter runAdapter;
  private CountDownLatch latch;
  private Signal signal;
}
