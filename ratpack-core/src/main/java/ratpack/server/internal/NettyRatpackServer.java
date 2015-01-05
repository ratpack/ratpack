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

package ratpack.server.internal;

import com.google.common.base.Throwables;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.error.internal.DefaultDevelopmentErrorHandler;
import ratpack.error.internal.DefaultProductionErrorHandler;
import ratpack.error.internal.ErrorHandler;
import ratpack.exec.ExecController;
import ratpack.exec.internal.DefaultExecController;
import ratpack.file.BaseDirRequiredException;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.file.internal.ActivationBackedMimeTypes;
import ratpack.file.internal.DefaultFileRenderer;
import ratpack.form.internal.FormParser;
import ratpack.func.Function;
import ratpack.handling.Redirector;
import ratpack.handling.internal.DefaultRedirector;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClients;
import ratpack.launch.LaunchException;
import ratpack.server.ServerConfig;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.render.internal.CharSequenceRenderer;
import ratpack.render.internal.PromiseRenderer;
import ratpack.render.internal.PublisherRenderer;
import ratpack.render.internal.RenderableRenderer;
import ratpack.server.PublicAddress;
import ratpack.server.RatpackServer;
import ratpack.server.Stopper;
import ratpack.util.internal.ChannelImplDetector;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static ratpack.util.ExceptionUtils.uncheck;
import static ratpack.util.internal.ProtocolUtil.HTTPS_SCHEME;
import static ratpack.util.internal.ProtocolUtil.HTTP_SCHEME;

public class NettyRatpackServer implements RatpackServer {

  private final Logger logger = LoggerFactory.getLogger(getClass().getName());

  private final Registry rootRegistry;
  private final ServerConfig serverConfig;
  private final Function<Stopper, ChannelInitializer<SocketChannel>> channelInitializerTransformer;

  private InetSocketAddress boundAddress;
  private Channel channel;

  private final Lock lifecycleLock = new ReentrantLock();
  private final AtomicBoolean running = new AtomicBoolean();

  public NettyRatpackServer(Registry rootRegistry, Function<Stopper, ChannelInitializer<SocketChannel>> channelInitializerTransformer) {
    this.rootRegistry = rootRegistry;
    this.serverConfig = rootRegistry.get(ServerConfig.class);
    this.channelInitializerTransformer = channelInitializerTransformer;
  }

  // TODO find better home for this
  public static Registry baseRegistry(ServerConfig serverConfig, Registry userRegistry) {
    ErrorHandler errorHandler = serverConfig.isDevelopment() ? new DefaultDevelopmentErrorHandler() : new DefaultProductionErrorHandler();
    ExecController execController = new DefaultExecController(serverConfig.getThreads());
    PooledByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;

    RegistryBuilder baseRegistry;
    try {
      baseRegistry = Registries.registry()
        .add(ServerConfig.class, serverConfig)
        .add(ByteBufAllocator.class, byteBufAllocator)
        .add(ExecController.class, execController)
        .add(MimeTypes.class, new ActivationBackedMimeTypes())
        .add(PublicAddress.class, new DefaultPublicAddress(serverConfig.getPublicAddress(), serverConfig.getSSLContext() == null ? HTTP_SCHEME : HTTPS_SCHEME))
        .add(Redirector.class, new DefaultRedirector())
        .add(ClientErrorHandler.class, errorHandler)
        .add(ServerErrorHandler.class, errorHandler)
        .with(new DefaultFileRenderer().register())
        .with(new PromiseRenderer().register())
        .with(new PublisherRenderer().register())
        .with(new RenderableRenderer().register())
        .with(new CharSequenceRenderer().register())
        .add(FormParser.class, FormParser.multiPart())
        .add(FormParser.class, FormParser.urlEncoded())
        .add(HttpClient.class, HttpClients.httpClient(execController, byteBufAllocator, serverConfig.getMaxContentLength()));
    } catch (Exception e) {
      // Uncheck because it really shouldn't happen
      throw uncheck(e);
    }

    if (serverConfig.isHasBaseDir()) {
      baseRegistry.add(FileSystemBinding.class, serverConfig.getBaseDir());
    }

    return baseRegistry.build().join(userRegistry);
  }

  @Override
  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  @Override
  public void start() throws LaunchException {
    lifecycleLock.lock();
    try {
      if (isRunning()) {
        return;
      }
      Stopper stopper = () -> {
        try {
          NettyRatpackServer.this.stop();
        } catch (Exception e) {
          throw uncheck(e);
        }
      };

      ServerBootstrap bootstrap = new ServerBootstrap();

      ChannelInitializer<SocketChannel> channelInitializer = channelInitializerTransformer.apply(stopper);

      bootstrap
        .group(rootRegistry.get(ExecController.class).getEventLoopGroup())
          .childHandler(channelInitializer)
        .channel(ChannelImplDetector.getServerSocketChannelImpl())
          .childOption(ChannelOption.ALLOCATOR, rootRegistry.get(ByteBufAllocator.class));

      if (System.getProperty("io.netty.leakDetectionLevel", null) == null) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
      }

      channel = bootstrap.bind(buildSocketAddress()).sync().channel();

      boundAddress = (InetSocketAddress) channel.localAddress();

      if (logger.isInfoEnabled()) {
        logger.info(String.format("Ratpack started for %s://%s:%s", getScheme(), getBindHost(), getBindPort()));
      }
      running.set(true);
    } catch (Exception e) {
      Throwables.propagateIfInstanceOf(e, BaseDirRequiredException.class);
      Throwables.propagateIfInstanceOf(e, LaunchException.class);
      throw new LaunchException("Unable to launch due to exception", e);
    } finally {
      lifecycleLock.unlock();
    }
  }

  @Override
  public void stop() throws Exception {
    lifecycleLock.lock();
    try {
      if (!isRunning()) {
        return;
      }
      channel.close();
      partialShutdown();
      running.set(false);
    } finally {
      lifecycleLock.unlock();
    }
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  private void partialShutdown() throws Exception {
    rootRegistry.get(ExecController.class).close();
  }

  @Override
  public String getScheme() {
    return serverConfig.getSSLContext() == null ? "http" : "https";
  }

  public int getBindPort() {
    return boundAddress == null ? -1 : boundAddress.getPort();
  }

  public String getBindHost() {
    if (boundAddress == null) {
      return null;
    } else {
      return HostUtil.determineHost(boundAddress);
    }
  }

  private InetSocketAddress buildSocketAddress() {
    return (serverConfig.getAddress() == null) ? new InetSocketAddress(serverConfig.getPort()) : new InetSocketAddress(serverConfig.getAddress(), serverConfig.getPort());
  }

}
