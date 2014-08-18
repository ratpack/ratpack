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

import com.google.common.base.Throwables;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import ratpack.file.BaseDirRequiredException;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.internal.FactoryHandler;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchException;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.server.internal.NettyRatpackServer;
import ratpack.server.internal.RatpackChannelInitializer;

import java.io.File;
import java.nio.file.Path;

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
    Function<Stopper, ChannelInitializer<SocketChannel>> channelInitializer = buildChannelInitializer(launchConfig);
    return new NettyRatpackServer(launchConfig, channelInitializer);
  }

  private static Function<Stopper, ChannelInitializer<SocketChannel>> buildChannelInitializer(final LaunchConfig launchConfig) {
    return new Function<Stopper, ChannelInitializer<SocketChannel>>() {
      @Override
      public ChannelInitializer<SocketChannel> apply(Stopper stopper) {
        return new RatpackChannelInitializer(launchConfig, createHandler(launchConfig), stopper);
      }
    };
  }

  private static Handler createHandler(final LaunchConfig launchConfig) {
    final HandlerFactory handlerFactory = launchConfig.getHandlerFactory();

    if (launchConfig.isDevelopment()) {
      File classFile = ClassUtil.getClassFile(handlerFactory);
      if (classFile != null) {
        Factory<Handler> factory = new ReloadableFileBackedFactory<>(classFile.toPath(), true, new ReloadableFileBackedFactory.Producer<Handler>() {
          @Override
          public Handler produce(Path file, ByteBuf bytes) {
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
      Throwables.propagateIfInstanceOf(e, BaseDirRequiredException.class);
      throw new LaunchException("Could not create handler via handler factory: " + handlerFactory.getClass().getName(), e);
    }
  }

}
