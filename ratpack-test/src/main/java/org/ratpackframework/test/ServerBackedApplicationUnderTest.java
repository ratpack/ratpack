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

import java.net.URI;
import java.net.URISyntaxException;

public class ServerBackedApplicationUnderTest implements ApplicationUnderTest {

  private RatpackServer server;
  private final Factory<RatpackServer> serverFactory;

  public ServerBackedApplicationUnderTest(Factory<RatpackServer> serverFactory) {
    this.serverFactory = serverFactory;
  }

  @Override
  public URI getAddress() {
    if (server == null) {
      server = serverFactory.create();
      try {
        server.start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    URI address;
    try {
      address = new URI(server.getScheme() + "://" + server.getBindHost() + ":" + server.getBindPort());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    return address;
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
