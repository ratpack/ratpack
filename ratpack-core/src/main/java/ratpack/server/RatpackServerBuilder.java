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

package ratpack.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import ratpack.handling.Handler;
import ratpack.handling.internal.FactoryHandler;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchException;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.server.internal.NettyRatpackService;
import ratpack.server.internal.RatpackChannelInitializer;
import ratpack.server.internal.ServiceBackedServer;
import ratpack.util.Factory;

import java.io.File;

/**
 * Builds a {@link RatpackServer}.
 */
public abstract class RatpackServerBuilder {

  private RatpackServerBuilder() {
  }

  /**
   * Constructs a new server based on the builder's state.
   * <p>
   * The returned server has not been started.
   *
   * @param launchConfig The server's configuration
   * @return A new, not yet started, Ratpack server.
   */
  public static RatpackServer build(LaunchConfig launchConfig) {
    ChannelInitializer<SocketChannel> channelInitializer = buildChannelInitializer(launchConfig);
    NettyRatpackService service = new NettyRatpackService(launchConfig, channelInitializer);
    return new ServiceBackedServer(service, launchConfig);
  }


  private static ChannelInitializer<SocketChannel> buildChannelInitializer(LaunchConfig launchConfig) {
    return new RatpackChannelInitializer(launchConfig, createHandler(launchConfig));
  }

  private static Handler createHandler(final LaunchConfig launchConfig) {
    final HandlerFactory handlerFactory = launchConfig.getHandlerFactory();

    if (launchConfig.isReloadable()) {
      File classFile = ClassUtil.getClassFile(handlerFactory);
      if (classFile != null) {
        Factory<Handler> factory = new ReloadableFileBackedFactory<>(classFile, true, new ReloadableFileBackedFactory.Producer<Handler>() {
          @Override
          public Handler produce(File file, ByteBuf bytes) {
            return createHandler(launchConfig, handlerFactory);
          }
        });
        return new FactoryHandler(factory);
      }
    }

    return createHandler(launchConfig, handlerFactory);
  }

  private static Handler createHandler(LaunchConfig launchConfig, HandlerFactory handlerFactory) {
    try {
      return handlerFactory.create(launchConfig);
    } catch (Exception e) {
      throw new LaunchException("Could not create handler via handler factory: " + handlerFactory.getClass().getName(), e);
    }
  }

}
