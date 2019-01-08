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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.api.Nullable;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import ratpack.exec.Throttle;
import ratpack.exec.internal.DefaultExecController;
import ratpack.exec.internal.ExecThreadBinding;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.http.internal.ConnectionIdleTimeout;
import ratpack.impose.Impositions;
import ratpack.impose.UserRegistryImposition;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ReloadInformant;
import ratpack.server.ServerConfig;
import ratpack.service.internal.DefaultEvent;
import ratpack.service.internal.ServicesGraph;
import ratpack.util.Exceptions;
import ratpack.util.Types;
import ratpack.util.internal.TransportDetector;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static ratpack.util.Exceptions.uncheck;

public class DefaultRatpackServer implements RatpackServer {

  public static final TypeToken<ReloadInformant> RELOAD_INFORMANT_TYPE = Types.token(ReloadInformant.class);
  public static final AttributeKey<Throttle> CHANNEL_THOTTLE_KEY = AttributeKey.valueOf(DefaultRatpackServer.class, "channelThottle");

  static {
    if (System.getProperty("io.netty.leakDetectionLevel", null) == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }
  }

  public static final TypeToken<HandlerDecorator> HANDLER_DECORATOR_TYPE_TOKEN = Types.token(HandlerDecorator.class);
  public static final Logger LOGGER = LoggerFactory.getLogger(RatpackServer.class);

  protected final Action<? super RatpackServerSpec> definitionFactory;

  protected InetSocketAddress boundAddress;
  protected Channel channel;
  protected DefaultExecController execController;
  protected Registry serverRegistry = Registry.empty();
  protected ServicesGraph servicesGraph;

  protected boolean reloading;
  protected final AtomicBoolean needsReload = new AtomicBoolean();

  protected boolean useSsl;
  private final Impositions impositions;

  @Nullable
  private Thread shutdownHookThread;

  public DefaultRatpackServer(Action<? super RatpackServerSpec> definitionFactory, Impositions impositions) throws Exception {
    this.definitionFactory = definitionFactory;
    this.impositions = impositions;

    ServerCapturer.capture(this);
  }

  @Override
  public synchronized void start() throws Exception {
    if (isRunning()) {
      return;
    }

    try {
      ServerConfig serverConfig;

      LOGGER.info("Starting server...");

      DefinitionBuild definitionBuild = buildUserDefinition();
      if (definitionBuild.error != null) {
        if (definitionBuild.getServerConfig().isDevelopment()) {
          LOGGER.warn("Exception raised getting server config (will use default config until reload):", definitionBuild.error);
          needsReload.set(true);
        } else {
          throw Exceptions.toException(definitionBuild.error);
        }
      }

      serverConfig = definitionBuild.getServerConfig();
      execController = new DefaultExecController(serverConfig.getThreads());
      ChannelHandler channelHandler = ExecThreadBinding.bindFor(true, execController, () -> buildHandler(definitionBuild));
      channel = buildChannel(serverConfig, channelHandler);

      boundAddress = (InetSocketAddress) channel.localAddress();

      String startMessage = String.format("Ratpack started %sfor %s://%s:%s", serverConfig.isDevelopment() ? "(development) " : "", getScheme(), getBindHost(), getBindPort());

      if (Slf4jNoBindingDetector.isHasBinding()) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(startMessage);
        }
      } else {
        System.out.println(startMessage);
      }

      if (serverConfig.isRegisterShutdownHook()) {
        shutdownHookThread = new Thread("ratpack-shutdown-thread") {
          @Override
          public void run() {
            try {
              DefaultRatpackServer.this.stop();
            } catch (Exception ignored) {
              ignored.printStackTrace(System.err);
            }
          }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
      }
    } catch (Exception e) {
      if (execController != null) {
        execController.close();
      }
      stop();
      throw e;
    }
  }

  private static class DefinitionBuild {
    private final Impositions impositions;
    private final RatpackServerDefinition definition;
    private final Throwable error;
    private final ServerConfig serverConfig;
    private final Function<? super Registry, ? extends Registry> userRegistryFactory;

    public DefinitionBuild(Impositions impositions, RatpackServerDefinition definition, Throwable error) {
      this.impositions = impositions;
      this.definition = definition;
      this.error = error;
      this.serverConfig = definition.getServerConfig();

      this.userRegistryFactory = baseRegistry -> {
        Registry userRegistry = definition.getRegistry().apply(baseRegistry);
        Registry userRegistryOverrides = impositions.get(UserRegistryImposition.class)
          .orElse(UserRegistryImposition.none())
          .build(userRegistry);

        return userRegistry.join(userRegistryOverrides);
      };
    }

    public Impositions getImpositions() {
      return impositions;
    }

    public ServerConfig getServerConfig() {
      return serverConfig;
    }

    public Function<? super Registry, ? extends Registry> getUserRegistryFactory() {
      return userRegistryFactory;
    }

    public Function<? super Registry, ? extends Handler> getHandlerFactory() {
      return definition.getHandler();
    }
  }

  protected DefinitionBuild buildUserDefinition() throws Exception {
    return Impositions.impose(impositions, () -> {
      try {
        return new DefinitionBuild(impositions, RatpackServerDefinition.build(definitionFactory), null);
      } catch (Exception e) {
        return new DefinitionBuild(impositions, RatpackServerDefinition.build(s -> s.handler(r -> ctx -> ctx.error(e))), e);
      }
    });
  }

  private ChannelHandler buildHandler(DefinitionBuild definitionBuild) throws Exception {
    if (definitionBuild.getServerConfig().isDevelopment()) {
      return new ReloadHandler(definitionBuild);
    } else {
      return buildAdapter(definitionBuild);
    }
  }

  protected Channel buildChannel(final ServerConfig serverConfig, final ChannelHandler handlerAdapter) throws InterruptedException {

    SslContext sslContext = serverConfig.getNettySslContext();
    this.useSsl = sslContext != null;

    ServerBootstrap serverBootstrap = new ServerBootstrap();

    serverConfig.getConnectTimeoutMillis().ifPresent(i -> {
      serverBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, i);
      serverBootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, i);
    });
    serverConfig.getMaxMessagesPerRead().ifPresent(i -> {
      FixedRecvByteBufAllocator allocator = new FixedRecvByteBufAllocator(i);
      serverBootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, allocator);
      serverBootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, allocator);
    });
    serverConfig.getReceiveBufferSize().ifPresent(i -> {
      serverBootstrap.option(ChannelOption.SO_RCVBUF, i);
      serverBootstrap.childOption(ChannelOption.SO_RCVBUF, i);
    });
    serverConfig.getWriteSpinCount().ifPresent(i -> {
      serverBootstrap.option(ChannelOption.WRITE_SPIN_COUNT, i);
      serverBootstrap.childOption(ChannelOption.WRITE_SPIN_COUNT, i);
    });
    serverConfig.getConnectQueueSize().ifPresent(i ->
      serverBootstrap.option(ChannelOption.SO_BACKLOG, i)
    );

    return serverBootstrap
      .group(execController.getEventLoopGroup())
      .channel(TransportDetector.getServerSocketChannelImpl())
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline pipeline = ch.pipeline();

          new ConnectionIdleTimeout(pipeline, serverConfig.getIdleTimeout());

          if (sslContext != null) {
            SSLEngine sslEngine = sslContext.newEngine(PooledByteBufAllocator.DEFAULT);
            pipeline.addLast("ssl", new SslHandler(sslEngine));
          }

          pipeline.addLast("decoder", new HttpRequestDecoder(
            serverConfig.getMaxInitialLineLength(),
            serverConfig.getMaxHeaderSize(),
            serverConfig.getMaxChunkSize(),
            false)
          );
          pipeline.addLast("encoder", new HttpResponseEncoder());
          pipeline.addLast("deflater", new IgnorableHttpContentCompressor());
          pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
          pipeline.addLast("adapter", handlerAdapter);

          ch.config().setAutoRead(false);
        }
      })
      .bind(buildSocketAddress(serverConfig))
      .sync()
      .channel();
  }

  protected NettyHandlerAdapter buildAdapter(DefinitionBuild definition) throws Exception {
    LOGGER.info("Building registry...");
    serverRegistry = buildServerRegistry(definition.getServerConfig(), definition.getUserRegistryFactory());

    Handler ratpackHandler = buildRatpackHandler(serverRegistry, definition.getHandlerFactory());
    ratpackHandler = decorateHandler(ratpackHandler, serverRegistry);

    servicesGraph = new ServicesGraph(serverRegistry);
    servicesGraph.start(new DefaultEvent(serverRegistry, reloading));
    return new NettyHandlerAdapter(serverRegistry, ratpackHandler);
  }

  private Registry buildServerRegistry(ServerConfig serverConfig, Function<? super Registry, ? extends Registry> userRegistryFactory) {
    return ServerRegistry.serverRegistry(this, impositions, execController, serverConfig, userRegistryFactory);
  }

  private Handler decorateHandler(Handler rootHandler, Registry serverRegistry) throws Exception {
    final Iterable<? extends HandlerDecorator> all = serverRegistry.getAll(HANDLER_DECORATOR_TYPE_TOKEN);
    for (HandlerDecorator handlerDecorator : all) {
      rootHandler = handlerDecorator.decorate(serverRegistry, rootHandler);
    }
    return rootHandler;
  }

  private Handler buildRatpackHandler(Registry serverRegistry, Function<? super Registry, ? extends Handler> handlerFactory) throws Exception {
    return handlerFactory.apply(serverRegistry);
  }

  @Override
  public synchronized void stop() throws Exception {
    if (!isRunning()) {
      return;
    }
    Channel lastChannel = channel;
    this.channel = null;

    try {
      if (shutdownHookThread != null) {
        Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
      }
    } catch (Exception ignored) {
      // just ignore
    }

    LOGGER.info("Stopping server...");

    if (lastChannel != null) {
      lastChannel.close().sync();
    }

    try {
      if (execController != null) {
        try {
          shutdownServices();
        } finally {
          execController.close();
        }
      }
    } finally {
      this.execController = null;
    }

    LOGGER.info("Server stopped.");
  }

  private void shutdownServices() throws Exception {
    if (servicesGraph != null) {
      servicesGraph.stop(new DefaultEvent(serverRegistry, reloading));
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
  public Optional<Registry> getRegistry() {
    return Optional.of(this.serverRegistry);
  }

  @Override
  public synchronized boolean isRunning() {
    return channel != null;
  }

  @Override
  public synchronized String getScheme() {
    return isRunning() ? this.useSsl ? "https" : "http" : null;
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

  @ChannelHandler.Sharable
  class ReloadHandler extends ChannelInboundHandlerAdapter {
    private ServerConfig lastServerConfig;
    private DefinitionBuild definitionBuild;
    private final Throttle reloadThrottle = Throttle.ofSize(1);

    private ChannelInboundHandlerAdapter inner;

    public ReloadHandler(DefinitionBuild definition) {
      super();
      this.definitionBuild = definition;
      this.lastServerConfig = definitionBuild.getServerConfig();
      try {
        this.inner = buildAdapter(definitionBuild);
      } catch (Exception e) {
        LOGGER.warn("An error occurred during startup. This will be ignored due to being in development mode.", e);
        this.inner = null;
        serverRegistry = buildServerRegistry(lastServerConfig, r -> r);
      }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      ctx.read();
      super.channelActive(ctx);
    }

    ChannelInboundHandlerAdapter getDelegate() {
      return inner;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (execController == null) {
        ReferenceCountUtil.release(msg);
        ctx.fireChannelReadComplete();
        return;
      }

      Throttle requestThrottle;
      if (msg instanceof HttpRequest) {
        requestThrottle = Throttle.ofSize(1);
        ctx.channel().attr(CHANNEL_THOTTLE_KEY).set(requestThrottle);
      } else {
        requestThrottle = ctx.channel().attr(CHANNEL_THOTTLE_KEY).get();
      }

      execController.fork().eventLoop(ctx.channel().eventLoop()).start(e ->
        Promise.<ChannelHandler>async(f -> {
          boolean rebuild = false;

          if (inner == null || definitionBuild.error != null) {
            rebuild = true;
          } else if (msg instanceof HttpRequest) {
            Optional<ReloadInformant> reloadInformant = serverRegistry.first(RELOAD_INFORMANT_TYPE, r -> r.shouldReload(serverRegistry) ? r : null);
            if (reloadInformant.isPresent()) {
              LOGGER.debug("reload requested by '" + reloadInformant.get() + "'");
              rebuild = true;
            }
          }

          if (rebuild) {
            Blocking.get(() -> {
              definitionBuild = buildUserDefinition();
              lastServerConfig = definitionBuild.getServerConfig();
              return buildAdapter(definitionBuild);
            })
              .wiretap(r -> {
                if (r.isSuccess()) {
                  inner = r.getValue();
                }
              })
              .mapError(this::buildErrorRenderingAdapter)
              .result(f::accept);
          } else {
            f.success(inner);
          }
        })
          .wiretap(r -> {
            try {
              ctx.pipeline().remove("inner");
            } catch (Exception ignore) {
              // ignore
            }
            ctx.pipeline().addLast("inner", r.getValueOrThrow());
          })
          .throttled(reloadThrottle)
          .throttled(requestThrottle)
          .then(adapter -> ctx.fireChannelRead(msg))
      );
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      if (inner == null) {
        super.channelRegistered(ctx);
      } else {
        inner.channelRegistered(ctx);
      }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
      if (inner == null) {
        super.channelUnregistered(ctx);
      } else {
        inner.channelUnregistered(ctx);
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      if (inner == null) {
        super.channelInactive(ctx);
      } else {
        inner.channelInactive(ctx);
      }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      if (inner == null) {
        super.channelReadComplete(ctx);
      } else {
        inner.channelReadComplete(ctx);
      }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (inner == null) {
        super.userEventTriggered(ctx, evt);
      } else {
        inner.userEventTriggered(ctx, evt);
      }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
      if (inner == null) {
        super.channelWritabilityChanged(ctx);
      } else {
        inner.channelWritabilityChanged(ctx);
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      if (inner == null) {
        super.exceptionCaught(ctx, cause);
      } else {
        inner.exceptionCaught(ctx, cause);
      }
    }

    private NettyHandlerAdapter buildErrorRenderingAdapter(Throwable e) {
      try {
        return new NettyHandlerAdapter(serverRegistry, context -> context.error(e));
      } catch (Exception e1) {
        throw uncheck(e);
      }
    }

  }

}
