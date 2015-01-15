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

public abstract class ServerCapture {

  private static final ThreadLocal<Registry> REGISTRY_HOLDER = ThreadLocal.withInitial(Registries::empty);
  private static final ThreadLocal<RatpackServer> SERVER_HOLDER = new ThreadLocal<>();

  private ServerCapture() {
  }

  public static Optional<RatpackServer> capture(Registry registry, NoArgAction bootstrap) throws Exception {
    REGISTRY_HOLDER.set(registry);
    try {
      bootstrap.execute();
    } finally {
      REGISTRY_HOLDER.remove();
    }

    RatpackServer ratpackServer = SERVER_HOLDER.get();
    SERVER_HOLDER.remove();
    return Optional.ofNullable(ratpackServer);
  }

  public static Registry capture(RatpackServer server) throws Exception {
    SERVER_HOLDER.set(server);
    return REGISTRY_HOLDER.get();
  }

}
