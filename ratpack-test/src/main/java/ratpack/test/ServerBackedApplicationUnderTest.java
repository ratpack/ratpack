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

package ratpack.test;

import ratpack.func.Factory;
import ratpack.server.RatpackServer;

import java.net.URI;
import java.net.URISyntaxException;

import static ratpack.util.ExceptionUtils.uncheck;

public class ServerBackedApplicationUnderTest implements CloseableApplicationUnderTest {

  private RatpackServer server;
  private final Factory<? extends RatpackServer> serverFactory;

  public ServerBackedApplicationUnderTest(Factory<? extends RatpackServer> serverFactory) {
    this.serverFactory = serverFactory;
  }

  @Override
  public URI getAddress() {
    if (server == null) {
      try {
        server = serverFactory.create();
        server.start();
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    URI address;
    try {
      address = new URI(server.getScheme() + "://" + server.getBindHost() + ":" + server.getBindPort() + "/");
    } catch (URISyntaxException e) {
      throw uncheck(e);
    }

    return address;
  }

  public void stop() {
    if (server != null) {
      try {
        server.stop();
        server = null;
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

  }

  public void close() {
    stop();
  }
}
