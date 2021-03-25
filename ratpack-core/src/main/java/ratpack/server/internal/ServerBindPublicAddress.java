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

package ratpack.server.internal;

import ratpack.core.server.PublicAddress;
import ratpack.core.server.RatpackServer;

import java.net.URI;

public class ServerBindPublicAddress implements PublicAddress {

  private final RatpackServer server;

  public ServerBindPublicAddress(RatpackServer server) {
    this.server = server;
  }

  @Override
  public URI get() {
    if (server.isRunning()) {
      String scheme = server.getScheme();
      int port = server.getBindPort();
      String portSuffix = scheme.equals("http") && port == 80 || scheme.equals("https") && port == 443 ? "" : ":" + port;
      return URI.create(scheme + "://" + server.getBindHost() + portSuffix);
    } else {
      throw new IllegalStateException("Server has not yet started");
    }
  }

}
