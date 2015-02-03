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
import ratpack.util.ExceptionUtils;
import ratpack.util.internal.ChannelImplDetector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static ratpack.util.ExceptionUtils.uncheck;

public class NettyRatpackServer implements RatpackServer {

  static {
    if (System.getProperty("io.netty.leakDetectionLevel", null) == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }
  }

  public static final TypeToken<HandlerDecorator> HANDLER_DECORATOR_TYPE_TOKEN = TypeToken.of(HandlerDecorator.class);
  private static final Logger LOGGER = LoggerFactory.getLogger(RatpackServer.class);

  protected final Function<? super Definition.Builder, ? extends Definition> definitionFactory;

  protected InetSocketAddress boundAddress;
  protected Channel channel;
  protected ExecController execController;
  protected Registry serverRegistry = Registries.empty();

  protected boolean reloading;
  protected final AtomicBoolean needsReload = new AtomicBoolean();

  protected SSLEngine sslEngine;
  private final ServerCapturer.Overrides overrides;

  public NettyRatpackServer(Function<? super Definition.Builder, ? extends Definition> definitionFactory) throws Exception {
    this.definitionFactory = definitionFactory;
    this.overrides = ServerCapturer.capture(this);
  }

  @Override
  public synchronized void start() throws Exception {
    if (isRunning()) {
      return;
    }

    ServerConfig serverConfig;

    DefinitionBuild definitionBuild = buildUserDefinition();
    if (definitionBuild.error != null) {
      if (definitionBuild.getServerConfig().isDevelopment()) {
        LOGGER.warn("Exception raised getting server config (will use default config until reload):", definitionBuild.error);
        needsReload.set(true);
      } else {
        throw ExceptionUtils.toException(definitionBuild.error);
      }
    }

    serverConfig = definitionBuild.getServerConfig();
    execController = new DefaultExecController(serverConfig.getThreads());
    ChannelHandler channelHandler = buildHandler(definitionBuild);
    channel = buildChannel(serverConfig, channelHandler);

    boundAddress = (InetSocketAddress) channel.localAddress();

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(String.format("Ratpack started for %s://%s:%s", getScheme(), getBindHost(), getBindPort()));
    }
  }

  private static class DefinitionBuild implements Definition {
    private final ServerCapturer.Overrides overrides;
    private final Definition definition;
    private final Throwable error;
    private final ServerConfig serverConfig;
    private final Function<? super Registry, ? extends Registry> userRegistryFactory;

    public DefinitionBuild(ServerCapturer.Overrides overrides, Definition definition, Throwable error) {
      this.overrides = overrides;
      this.definition = definition;
      this.error = error;
      this.serverConfig = new DelegatingServerConfig(definition.getServerConfig()) {
        @Override
        public int getPort() {
          return overrides.getPort() < 0 ? super.getPort() : overrides.getPort();
        }

        @Override
        public boolean isDevelopment() {
          return overrides.isDevelopment() == null ? super.isDevelopment() : overrides.isDevelopment();
        }
      };

      Registry serverConfigOverrideRegistry = Registries.just(ServerConfig.class, serverConfig);
      this.userRegistryFactory = baseRegistry -> {
        Registry actualBaseRegistry = baseRegistry.join(serverConfigOverrideRegistry);
        Registry userRegistry = definition.getUserRegistryFactory().apply(actualBaseRegistry);
        Registry overrideRegistry = overrides.getRegistryFunction().apply(userRegistry);
        return userRegistry.join(overrideRegistry);
      };
    }

    public ServerCapturer.Overrides getOverrides() {
      return overrides;
    }

    @Override
    public ServerConfig getServerConfig() {
      return serverConfig;
    }

    @Override
    public Function<? super Registry, ? extends Registry> getUserRegistryFactory() {
      return userRegistryFactory;
    }

    @Override
    public Function<? super Registry, ? extends Handler> getHandlerFactory() {
      return definition.getHandlerFactory();
    }
  }

  protected DefinitionBuild buildUserDefinition() throws Exception {
    try {
      return new DefinitionBuild(overrides, definitionFactory.apply(new DefaultRatpackServerDefinitionBuilder()), null);
    } catch (Exception e) {
      return new DefinitionBuild(overrides, new DefaultServerDefinition(ServerConfig.noBaseDir().build(), r -> Registries.empty(), r -> ctx -> ctx.error(e)), e);
    }
  }

  private ChannelHandler buildHandler(DefinitionBuild definitionBuild) throws Exception {
    if (definitionBuild.getServerConfig().isDevelopment()) {
      return new ReloadHandler(definitionBuild);
    } else {
      return buildAdapter(definitionBuild);
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

  protected NettyHandlerAdapter buildAdapter(DefinitionBuild definition) throws Exception {
    serverRegistry = buildServerRegistry(definition.getServerConfig(), definition.getUserRegistryFactory());

    Handler ratpackHandler = buildRatpackHandler(definition.getServerConfig(), serverRegistry, definition.getHandlerFactory());
    ratpackHandler = decorateHandler(ratpackHandler, serverRegistry);

    executeEvents(serverRegistry, StartEvent.build(serverRegistry, reloading), Service::onStart);

    return new NettyHandlerAdapter(definition.getServerConfig(), serverRegistry, ratpackHandler);
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
        executeEvents(serverRegistry, StopEvent.build(serverRegistry, reloading), Service::onStop);
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

  private <E> void executeEvents(Registry registry, E event, BiAction<Service, E> action) throws Exception {
    for (Service service : registry.getAll(Service.class)) {
      action.execute(service, event);
    }
  }

  @ChannelHandler.Sharable
  private class ReloadHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private ServerConfig lastServerConfig;
    private DefinitionBuild definitionBuild;
    private final Lock reloadLock = new ReentrantLock();

    private ChannelHandler inner;

    public ReloadHandler(DefinitionBuild definition) {
      super(false);
      this.definitionBuild = definition;
      this.lastServerConfig = definitionBuild.getServerConfig();
      try {
        this.inner = buildAdapter(definitionBuild);
      } catch (Exception e) {
        this.inner = null;
      }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
      reloadLock.lock();
      try {
        boolean rebuild = false;

        if (inner == null || definitionBuild.error != null) {
          rebuild = true;
        } else {
          Optional<ReloadInformant> reloadInformant = serverRegistry.first(TypeToken.of(ReloadInformant.class), r -> r.shouldReload(serverRegistry));
          if (reloadInformant.isPresent()) {
            LOGGER.warn("reload requested by '" + reloadInformant.get() + "'");
            rebuild = true;
          }
        }

        if (rebuild) {
          try {
            definitionBuild = buildUserDefinition();
            lastServerConfig = definitionBuild.getServerConfig();
            inner = buildAdapter(definitionBuild);
            delegate(ctx, inner, msg);
          } catch (Exception e) {
            delegate(ctx, buildErrorRenderingAdapter(e), msg);
          }
        } else {
          delegate(ctx, inner, msg);
        }
      } finally {
        reloadLock.unlock();
      }
    }

    private NettyHandlerAdapter buildErrorRenderingAdapter(Exception e) {
      try {
        return new NettyHandlerAdapter(lastServerConfig, buildServerRegistry(lastServerConfig, (r) -> Registries.empty()), context -> context.error(e));
      } catch (Exception e1) {
        throw uncheck(e);
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
