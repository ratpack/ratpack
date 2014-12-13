/*
 * Copyright 2014 the original author or authors.
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

package ratpack.launch;

import static ratpack.util.ExceptionUtils.uncheck;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ratpack.exec.ExecController;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.Action;
import ratpack.registry.*;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerBuilder;

/**
 * Creates and configures a Ratpack application.
 *
 * <pre class="java">{@code
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.server.RatpackServer;
 * import ratpack.launch.RatpackLauncher;
 *
 * public class Example {
 *  public static class MyHandler implements Handler {
 *    public void handle(Context context) throws Exception {
 *    }
 *  }
 *
 *  public static void main(String[] args) throws Exception {
 *    RatpackServer server = RatpackLauncher.launcher(r -> {
 *      r.add(MyHandler.class, new MyHandler());
 *    }).config( c -> {
 *      c.port(5060);
 *    }).build(registry -> {
 *      return registry.get(MyHandler.class);
 *    });
 *    server.start();
 *
 *    assert server.isRunning();
 *    assert server.getBindPort() == 5060;
 *
 *    server.stop();
 *  }
 * }
 * }</pre>
 */
public abstract class RatpackLauncher {

  public static RatpackLauncher launcher(Action<? super RegistrySpec> action) {
    try {
      Registry baseRegistry = baseRegistry();
      RegistryBuilder builder = Registries.registry();
      action.execute(builder);
      return new DefaultRatpackLauncher(baseRegistry.join(builder.build()));
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public static Registry baseRegistry() {
    RegistryBuilder builder = Registries.registry();
    builder.add(ServerConfig.class, ServerConfigBuilder.noBaseDir().build());
    builder.add(ByteBufAllocator.class, PooledByteBufAllocator.DEFAULT);

    //TODO-JOHN this is ugly and also how do this respond to someone changing the # of threads using the config method below?
    Registry registry = builder.build();
    builder.add(ExecController.class, new DefaultExecController(registry.get(ServerConfig.class).getThreads()));

    return builder.build();
  }

  public abstract RatpackServer build(HandlerFactory handlerFactory);

  public abstract RatpackLauncher config(Action<? super ServerConfigSpec> action);

  static class DefaultRatpackLauncher extends RatpackLauncher {

    private Registry baseRegistry;

    DefaultRatpackLauncher(Registry baseRegistry) {
      this.baseRegistry = baseRegistry;
    }

    @Override
    public RatpackServer build(HandlerFactory handlerFactory) {
      try {
        return RatpackServerBuilder.build(baseRegistry, handlerFactory);
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    @Override
    public RatpackLauncher config(Action<? super ServerConfigSpec> action) {
      try {
        ServerConfigBuilder builder = ServerConfigBuilder.noBaseDir();
        action.execute(builder);
        baseRegistry = baseRegistry.join(Registries.just(ServerConfig.class, builder.build()));
        return this;
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

  }

}
