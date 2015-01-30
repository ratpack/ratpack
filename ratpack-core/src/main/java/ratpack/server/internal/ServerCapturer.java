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

package ratpack.server.internal;

import ratpack.func.NoArgAction;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;

import java.util.Optional;

public abstract class ServerCapturer {

  private static final ThreadLocal<Overrides> OVERRIDES_HOLDER = ThreadLocal.withInitial(Overrides::new);
  private static final ThreadLocal<RatpackServer> SERVER_HOLDER = new ThreadLocal<>();

  private ServerCapturer() {
  }

  public static class Overrides {
    private final int port;
    private final Registry registry;

    public Overrides(Registry registry) {
      this(-1, registry);
    }

    public Overrides() {
      this(-1, Registries.empty());
    }

    public Overrides(int port) {
      this(port, Registries.empty());
    }

    public Overrides(int port, Registry registry) {
      this.port = port;
      this.registry = registry;
    }

    public int getPort() {
      return port;
    }

    public Registry getRegistry() {
      return registry;
    }
  }

  public static Optional<RatpackServer> capture(Overrides overrides, NoArgAction bootstrap) throws Exception {
    OVERRIDES_HOLDER.set(overrides);
    try {
      bootstrap.execute();
    } finally {
      OVERRIDES_HOLDER.remove();
    }

    RatpackServer ratpackServer = SERVER_HOLDER.get();
    SERVER_HOLDER.remove();
    return Optional.ofNullable(ratpackServer);
  }

  public static Overrides capture(RatpackServer server) throws Exception {
    SERVER_HOLDER.set(server);
    return OVERRIDES_HOLDER.get();
  }

}
