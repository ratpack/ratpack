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
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.ExecController;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.BiAction;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.internal.FactoryHandler;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.server.*;
import ratpack.util.internal.ChannelImplDetector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static ratpack.util.ExceptionUtils.uncheck;

public class NettyRatpackServer implements RatpackServer {

  static {
    if (System.getProperty("io.netty.leakDetectionLevel", null) == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }
  }

  public static final TypeToken<HandlerDecorator> HANDLER_DECORATOR_TYPE_TOKEN = TypeToken.of(HandlerDecorator.class);

  protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
  protected final Function<? super Definition.Builder, ? extends Definition> definitionFactory;

  protected InetSocketAddress boundAddress;
  protected Channel channel;
  protected ExecController execController;
  protected Registry serverRegistry;

  protected boolean reloading;
  protected final AtomicBoolean needsReload = new AtomicBoolean();

  protected SSLEngine sslEngine;

  public NettyRatpackServer(Function<? super Definition.Builder, ? extends Definition> definitionFactory) {
    this.definitionFactory = definitionFactory;
  }

  @Override
  public synchronized void start() throws Exception {
    if (isRunning()) {
      return;
    }

    ServerConfig serverConfig;

    DefinitionBuild definitionBuild;
    try {
      definitionBuild = buildUserDefinition();
    } catch (Exception e) {
      serverConfig = ServerConfig.noBaseDir().build();
      if (!serverConfig.isDevelopment()) {
        throw e;
      }

      logger.warn("Exception raised getting server config, using default config for development", e);
      definitionBuild = buildErrorPageDefinition(e, serverConfig);
      needsReload.set(true);
    }

    serverConfig = definitionBuild.definition.getServerConfig();

    execController = new DefaultExecController(serverConfig.getThreads());

    ChannelHandler channelHandler = buildHandler(definitionBuild, serverConfig);
    channel = buildChannel(serverConfig, channelHandler);

    boundAddress = (InetSocketAddress) channel.localAddress();

    if (logger.isInfoEnabled()) {
      logger.info(String.format("Ratpack started for %s://%s:%s", getScheme(), getBindHost(), getBindPort()));
    }
  }

  private static class DefinitionBuild {
    private final Definition definition;
    private final boolean inError;

    public DefinitionBuild(Definition definition, boolean inError) {
      this.definition = definition;
      this.inError = inError;
    }
  }

  protected DefinitionBuild buildErrorPageDefinition(Exception e, ServerConfig defaultServerConfig) {
    return new DefinitionBuild(new DefaultServerDefinition(defaultServerConfig, r -> Registries.empty(), r -> ctx -> ctx.error(e)), true);
  }

  protected DefinitionBuild buildUserDefinition() throws Exception {
    return new DefinitionBuild(definitionFactory.apply(new DefaultRatpackServerDefinitionBuilder()), false);
  }

  private ChannelHandler buildHandler(DefinitionBuild definitionBuild, ServerConfig serverConfig) throws Exception {
    if (serverConfig.isDevelopment()) {
      return new ReloadHandler(definitionBuild, serverConfig);
    } else {
      return buildAdapter(definitionBuild.definition, serverConfig);
    }
  }

  protected Channel buildChannel(final ServerConfig serverConfig, final ChannelHandler handlerAdapter) throws InterruptedException {

    SSLContext sslContext = serverConfig.getSSLContext();
    if (sslContext != null) {
      this.sslEngine = sslContext.createSSLEngine();
      sslEngine.setUseClientMode(false);
    }

    return new ServerBootstrap()
      .group(execController.getEventLoopGroup())
      .channel(ChannelImplDetector.getServerSocketChannelImpl())
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline pipeline = ch.pipeline();
          if (sslContext != null) {
            pipeline.addLast("ssl", new SslHandler(sslEngine));
          }

          pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false));
          pipeline.addLast("aggregator", new HttpObjectAggregator(serverConfig.getMaxContentLength()));
          pipeline.addLast("encoder", new HttpResponseEncoder());
          if (serverConfig.isCompressResponses()) {
            pipeline.addLast("deflater", new SmartHttpContentCompressor());
          }

          pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
          pipeline.addLast("adapter", handlerAdapter);
        }
      })
      .bind(buildSocketAddress(serverConfig))
      .sync()
      .channel();
  }

  protected NettyHandlerAdapter buildAdapter(Definition definition, ServerConfig serverConfig) throws Exception {
    ServerCapturer.Overrides overrides = ServerCapturer.capture(this);
    Registry definedServerRegistry = buildServerRegistry(serverConfig, definition.getUserRegistryFactory());
    serverRegistry = definedServerRegistry.join(overrides.getRegistry());

    Handler ratpackHandler = buildRatpackHandler(serverConfig, serverRegistry, definition.getHandlerFactory());
    ratpackHandler = decorateHandler(ratpackHandler, serverRegistry);

    executeEvents(serverRegistry, StartEvent.build(serverRegistry, reloading), ServerLifecycleListener::onStart);

    return new NettyHandlerAdapter(serverConfig, serverRegistry, ratpackHandler);
  }

  private Registry buildServerRegistry(ServerConfig serverConfig, Function<? super Registry, ? extends Registry> userRegistryFactory) {
    return ServerRegistry.serverRegistry(this, execController, serverConfig, userRegistryFactory);
  }

  private Handler decorateHandler(Handler rootHandler, Registry serverRegistry) {
    for (HandlerDecorator handlerDecorator : serverRegistry.getAll(HANDLER_DECORATOR_TYPE_TOKEN)) {
      rootHandler = handlerDecorator.decorate(serverRegistry, rootHandler);
    }
    return rootHandler;
  }

  private Handler buildRatpackHandler(ServerConfig serverConfig, Registry serverRegistry, Function<? super Registry, ? extends Handler> handlerFactory) throws Exception {
    if (serverConfig.isDevelopment()) {
      File classFile = ClassUtil.getClassFile(handlerFactory);
      if (classFile != null) {
        Factory<Handler> factory = new ReloadableFileBackedFactory<>(classFile.toPath(), true, (file, bytes) -> handlerFactory.apply(this.serverRegistry));
        return new FactoryHandler(factory);
      }
    }

    return handlerFactory.apply(serverRegistry);
  }

  @Override
  public synchronized void stop() throws Exception {
    try {
      if (!isRunning()) {
        return;
      }
      if (serverRegistry != null) {
        executeEvents(serverRegistry, StopEvent.build(serverRegistry, reloading), ServerLifecycleListener::onStop);
      }
    } finally {
      Optional.ofNullable(channel).ifPresent(Channel::close);
      Optional.ofNullable(execController).ifPresent(ExecController::close);
      channel = null;
      execController = null;
    }
  }

  @Override
  public synchronized RatpackServer reload() throws Exception {
    reloading = true;
    boolean start = false;

    if (this.isRunning()) {
      start = true;
      this.stop();
    }

    if (start) {
      start();
    }

    reloading = false;
    return this;
  }

  @Override
  public synchronized boolean isRunning() {
    return channel != null;
  }

  @Override
  public synchronized String getScheme() {
    return isRunning() ? sslEngine == null ? "http" : "https" : null;
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

  private InetSocketAddress buildSocketAddress(ServerConfig serverConfig) {
    return (serverConfig.getAddress() == null) ? new InetSocketAddress(serverConfig.getPort()) : new InetSocketAddress(serverConfig.getAddress(), serverConfig.getPort());
  }

  private <E> void executeEvents(Registry registry, E event, BiAction<ServerLifecycleListener, E> action) throws Exception {
    registry.getAll(ServerLifecycleListener.class).forEach(listener -> uncheck(listener, event, action));
  }

  @ChannelHandler.Sharable
  private class ReloadHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private DefinitionBuild definitionBuild;
    private final ServerConfig serverConfig;

    private ChannelHandler inner;

    public ReloadHandler(DefinitionBuild definition, ServerConfig serverConfig) {
      super(false);
      this.definitionBuild = definition;
      this.serverConfig = serverConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
      if (inner == null || definitionBuild.inError) {
        try {
          if (definitionBuild.inError) {
            definitionBuild = buildUserDefinition();
          }
          inner = buildAdapter(definitionBuild.definition, serverConfig);
          delegate(ctx, inner, msg);
        } catch (Exception e) {
          delegate(ctx, new NettyHandlerAdapter(serverConfig, buildServerRegistry(serverConfig, (r) -> Registries.empty()), context -> context.error(e)), msg);
        }
      } else {
        delegate(ctx, inner, msg);
      }
    }

    private void delegate(ChannelHandlerContext ctx, ChannelHandler delegate, FullHttpRequest msg) {
      try {
        ctx.pipeline().remove("inner");
      } catch (Exception ignore) {
        // ignore
      }
      ctx.pipeline().addLast("inner", delegate);
      ctx.fireChannelRead(msg);
    }
  }
}
