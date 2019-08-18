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

package ratpack.gradle.continuous.run

import org.gradle.api.Action
import org.gradle.internal.UncheckedException

import java.util.concurrent.CountDownLatch

class RatpackWorkerServer implements Action/*<WorkerProcessContext>*/, RatpackAdapter, Serializable {

  private final RatpackAdapter runAdapter
  private CountDownLatch latch
  private Signal signal

  RatpackWorkerServer(RatpackAdapter runAdapter) {
    this.runAdapter = runAdapter
  }

  void execute(Object context) {
    disableUrlConnectionCaching()
    context.getServerConnection().addIncoming(RatpackAdapter, this)
    signal = context.getServerConnection().addOutgoing(Signal)
    context.getServerConnection().connect()
    latch = new CountDownLatch(1)
    latch.await()
  }

  @Override
  void start() {
    runAdapter.start()
    signal.fire()
  }

  @Override
  void reload() {
    runAdapter.reload()
    signal.fire()
  }

  @Override
  boolean isRunning() {
    return runAdapter.isRunning()
  }

  @Override
  void stop() {
    latch.countDown()
    runAdapter.stop()
  }

  @Override
  void buildError(Throwable throwable) {
    runAdapter.buildError(throwable)
    signal.fire()
  }

  private static void disableUrlConnectionCaching() {
    // fix problems in updating jar files by disabling default caching of URL connections.
    // URLConnection default caching should be disabled since it causes jar file locking issues and JVM crashes in updating jar files.
    // Changes to jar files won't be noticed in all cases when caching is enabled.
    // sun.net.www.protocol.jar.JarURLConnection leaves the JarFile instance open if URLConnection caching is enabled.
    try {
      URL url = new URL("jar:file://valid_jar_url_syntax.jar!/")
      URLConnection urlConnection = url.openConnection()
      urlConnection.setDefaultUseCaches(false)
    } catch (MalformedURLException e) {
      throw UncheckedException.throwAsUncheckedException(e)
    } catch (IOException e) {
      throw UncheckedException.throwAsUncheckedException(e)
    }
  }

}
