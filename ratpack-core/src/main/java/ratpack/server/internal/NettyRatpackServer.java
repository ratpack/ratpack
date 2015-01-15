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

import com.google.common.reflect.TypeToken;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.ExecController;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.internal.FactoryHandler;
import ratpack.registry.Registry;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.util.internal.ChannelImplDetector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NettyRatpackServer implements RatpackServer {

  public static final TypeToken<HandlerDecorator> HANDLER_DECORATOR_TYPE_TOKEN = TypeToken.of(HandlerDecorator.class);
  private final Logger logger = LoggerFactory.getLogger(getClass().getName());

  private final Function<? super Definition.Builder, ? extends Definition> definitionFactory;
  private Definition definition;
  private Registry serverRegistry;

  private InetSocketAddress boundAddress;
  private Channel channel;

  private final Lock lifecycleLock = new ReentrantLock();
  private final AtomicBoolean running = new AtomicBoolean();

  public NettyRatpackServer(Function<? super Definition.Builder, ? extends Definition> definitionFactory) throws Exception {
    this.definitionFactory = definitionFactory;
    define();
  }

  private void define() throws Exception {
    definition = definitionFactory.apply(new DefaultRatpackServerDefinitionBuilder());
    serverRegistry = ServerRegistry.serverRegistry(definition.getServerConfig(), this, definition.getUserRegistryFactory());
  }

  @Override
  public synchronized ServerConfig getServerConfig() {
    return definition.getServerConfig();
  }

  @Override
  public synchronized void start() throws Exception {
    if (isRunning()) {
      return;
    }

    if (System.getProperty("io.netty.leakDetectionLevel", null) == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }

    Handler rootHandler = buildRootHandler();
    Handler decorateHandler = decorateHandler(rootHandler, serverRegistry);
    NettyHandlerAdapter handlerAdapter = new NettyHandlerAdapter(getServerConfig(), serverRegistry, decorateHandler);

    SSLContext sslContext = getServerConfig().getSSLContext();
    SSLEngine sslEngine = null;
    if (sslContext != null) {
      sslEngine = sslContext.createSSLEngine();
      sslEngine.setUseClientMode(false);
    }

    final SSLEngine finalSslEngine = sslEngine;

    channel = new ServerBootstrap()
      .group(serverRegistry.get(ExecController.class).getEventLoopGroup())
      .channel(ChannelImplDetector.getServerSocketChannelImpl())
      .childOption(ChannelOption.ALLOCATOR, serverRegistry.get(ByteBufAllocator.class))
      .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline pipeline = ch.pipeline();

          if (finalSslEngine != null) {
            pipeline.addLast("ssl", new SslHandler(finalSslEngine));
          }

          pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false));
          pipeline.addLast("aggregator", new HttpObjectAggregator(getServerConfig().getMaxContentLength()));
          pipeline.addLast("encoder", new HttpResponseEncoder());
          if (getServerConfig().isCompressResponses()) {
            pipeline.addLast("deflater", new SmartHttpContentCompressor());
          }
          pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
          pipeline.addLast("rootHandler", handlerAdapter);
        }
      })
      .bind(buildSocketAddress())
      .sync()
      .channel();

    boundAddress = (InetSocketAddress) channel.localAddress();

    if (logger.isInfoEnabled()) {
      logger.info(String.format("Ratpack started for %s://%s:%s", getScheme(), getBindHost(), getBindPort()));
    }
    running.set(true);
  }

  private Handler decorateHandler(Handler rootHandler, Registry serverRegistry) {
    for (HandlerDecorator handlerDecorator : serverRegistry.getAll(HANDLER_DECORATOR_TYPE_TOKEN)) {
      rootHandler = handlerDecorator.decorate(rootHandler);
    }
    return rootHandler;
  }

  private Handler buildRootHandler() throws Exception {
    if (getServerConfig().isDevelopment()) {
      File classFile = ClassUtil.getClassFile(definition.getHandlerFactory());
      if (classFile != null) {
        Factory<Handler> factory = new ReloadableFileBackedFactory<>(classFile.toPath(), true, (file, bytes) -> definition.getHandlerFactory().apply(serverRegistry));
        return new FactoryHandler(factory);
      }
    }

    return definition.getHandlerFactory().apply(serverRegistry);
  }

  @Override
  public synchronized void stop() throws Exception {
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
  public synchronized RatpackServer reload() throws Exception {
    boolean start = false;
    if (this.isRunning()) {
      start = true;
      this.stop();
    }

    define();

    if (start) {
      start();
    }

    return this;
  }

  @Override
  public synchronized boolean isRunning() {
    return running.get();
  }

  private void partialShutdown() throws Exception {
    serverRegistry.get(ExecController.class).close();
  }

  @Override
  public synchronized String getScheme() {
    return getServerConfig().getSSLContext() == null ? "http" : "https";
  }

  public synchronized int getBindPort() {
    return boundAddress == null ? -1 : boundAddress.getPort();
  }

  public synchronized String getBindHost() {
    if (boundAddress == null) {
      return null;
    } else {
      return HostUtil.determineHost(boundAddress);
    }
  }

  private InetSocketAddress buildSocketAddress() {
    ServerConfig serverConfig = getServerConfig();
    return (serverConfig.getAddress() == null) ? new InetSocketAddress(serverConfig.getPort()) : new InetSocketAddress(serverConfig.getAddress(), serverConfig.getPort());
  }

}
