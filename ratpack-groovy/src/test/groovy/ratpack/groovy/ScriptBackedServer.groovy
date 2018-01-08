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

package ratpack.groovy

import ratpack.impose.ForceServerListenPortImposition
import ratpack.impose.Impositions
import ratpack.registry.Registry
import ratpack.server.RatpackServer
import ratpack.server.StartupFailureException
import ratpack.server.internal.ServerCapturer

class ScriptBackedServer implements RatpackServer {

  private final Runnable starter

  private RatpackServer nestedServer

  ScriptBackedServer(Runnable starter) {
    this.starter = starter
  }


  @Override
  void start() throws StartupFailureException {
    nestedServer = Impositions.of { it.add(ForceServerListenPortImposition.ephemeral()) }.impose { ->
      ServerCapturer.capture({ -> starter.run() })
    }

    def stopAt = System.currentTimeMillis() + 10000
    while (System.currentTimeMillis() < stopAt && (nestedServer == null || !nestedServer.running)) {
      sleep 100
    }

    if (!nestedServer) {
      throw new IllegalStateException("Server did not start")
    }
  }

  @Override
  void stop() throws Exception {
    nestedServer?.stop()
  }

  @Override
  RatpackServer reload() throws Exception {
    return nestedServer?.reload()
  }

  @Override
  Optional<Registry> getRegistry() {
    return nestedServer.registry
  }

  @Override
  boolean isRunning() {
    nestedServer != null && nestedServer.running
  }

  @Override
  String getScheme() {
    nestedServer.scheme
  }

  @Override
  int getBindPort() {
    nestedServer.bindPort
  }

  @Override
  String getBindHost() {
    nestedServer.bindHost
  }
}
