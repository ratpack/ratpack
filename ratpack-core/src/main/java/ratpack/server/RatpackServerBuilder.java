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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import ratpack.file.BaseDirRequiredException;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.internal.FactoryHandler;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchException;
import ratpack.launch.ServerConfig;
import ratpack.registry.Registry;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.server.internal.NettyRatpackServer;
import ratpack.server.internal.RatpackChannelInitializer;

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
   * @param rootRegistry the base registry containing the ServerConfig and items used to initialize the server
   * @param handlerFactory the application's root handler
   *
   * @return A new, not yet started, Ratpack server.
   */
  public static RatpackServer build(Registry rootRegistry, HandlerFactory handlerFactory) {
    Function<Stopper, ChannelInitializer<SocketChannel>> channelInitializer = buildChannelInitializer(handlerFactory, rootRegistry);
    return new NettyRatpackServer(rootRegistry, channelInitializer);
  }

  private static Function<Stopper, ChannelInitializer<SocketChannel>> buildChannelInitializer(final HandlerFactory handlerFactory, final Registry rootRegistry) {
    return stopper -> new RatpackChannelInitializer(rootRegistry, createHandler(handlerFactory, rootRegistry, rootRegistry.get(ServerConfig.class)), stopper);
  }

  private static Handler createHandler(final HandlerFactory handlerFactory, final Registry rootRegistry, final ServerConfig serverConfig) {

    if (serverConfig.isDevelopment()) {
      File classFile = ClassUtil.getClassFile(handlerFactory);
      if (classFile != null) {
        Factory<Handler> factory = new ReloadableFileBackedFactory<>(classFile.toPath(), true, (file, bytes) -> createHandler(rootRegistry, handlerFactory));
        return new FactoryHandler(factory);
      }
    }

    return createHandler(rootRegistry, handlerFactory);
  }

  private static Handler createHandler(Registry rootRegistry, HandlerFactory handlerFactory) {
    try {
      return handlerFactory.create(rootRegistry);
    } catch (Exception e) {
      Throwables.propagateIfInstanceOf(e, BaseDirRequiredException.class);
      throw new LaunchException("Could not create handler via handler factory: " + handlerFactory.getClass().getName(), e);
    }
  }

}
