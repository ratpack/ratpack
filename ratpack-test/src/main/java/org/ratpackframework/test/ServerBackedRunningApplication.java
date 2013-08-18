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

package org.ratpackframework.test;

import org.ratpackframework.server.RatpackServer;
import org.ratpackframework.util.Factory;

public class ServerBackedRunningApplication implements RunningApplication {

  private RatpackServer server;
  private final Factory<RatpackServer> serverFactory;

  public ServerBackedRunningApplication(Factory<RatpackServer> serverFactory) {
    this.serverFactory = serverFactory;
  }

  @Override
  public String getAddress() {
    if (server == null) {
      server = serverFactory.create();
      try {
        server.start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return "http://" + server.getBindHost() + ":" + server.getBindPort();
  }

  public void stop() {
    if (server != null) {
      try {
        server.stop();
        server = null;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

  }
}
