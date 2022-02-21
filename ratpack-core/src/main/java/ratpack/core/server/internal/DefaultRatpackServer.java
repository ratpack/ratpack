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

package ratpack.core.server.internal;

import com.google.common.reflect.TypeToken;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import io.netty.util.Mapping;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.func.Nullable;
import ratpack.exec.Blocking;
import ratpack.exec.ExecController;
import ratpack.exec.Promise;
import ratpack.exec.Throttle;
import ratpack.exec.internal.DefaultExecController;
import ratpack.exec.internal.ExecControllerInternal;
import ratpack.exec.internal.ExecThreadBinding;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.core.handling.Handler;
import ratpack.core.handling.HandlerDecorator;
import ratpack.core.http.internal.ConnectionIdleTimeout;
import ratpack.core.impose.Impositions;
import ratpack.core.impose.UserRegistryImposition;
import ratpack.exec.registry.Registry;
import ratpack.core.server.RatpackServer;
import ratpack.core.server.RatpackServerSpec;
import ratpack.core.server.ReloadInformant;
import ratpack.core.server.ServerConfig;
import ratpack.core.service.internal.DefaultEvent;
import ratpack.core.service.internal.ServicesGraph;
import ratpack.func.Exceptions;
import ratpack.func.Types;
import ratpack.exec.util.internal.TransportDetector;

import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ratpack.func.Exceptions.uncheck;

public class DefaultRatpackServer implements RatpackServer {

  public static final TypeToken<ReloadInformant> RELOAD_INFORMANT_TYPE = Types.token(ReloadInformant.class);
  public static final AttributeKey<Throttle> CHANNEL_THROTTLE_KEY = AttributeKey.valueOf(DefaultRatpackServer.class, "channelThrottle");

  static {
    if (System.getProperty("io.netty.leakDetectionLevel", null) == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }
  }

  public static final TypeToken<HandlerDecorator> HANDLER_DECORATOR_TYPE_TOKEN = Types.token(HandlerDecorator.class);
  public static final Logger LOGGER = LoggerFactory.getLogger(RatpackServer.class);

  protected final Action<? super RatpackServerSpec> definitionFactory;

  private static final class RunningState {

    private final InetSocketAddress boundAddress;
    private final Channel channel;
    private final ExecControllerInternal execController;
    private final boolean useSsl;
    private final DefaultRatpackServer.ApplicationState applicationState;
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    RunningState(
      InetSocketAddress boundAddress,
      Channel channel,
      ExecControllerInternal execController,
      boolean useSsl,
      DefaultRatpackServer.ApplicationState applicationState
    ) {
      this.boundAddress = boundAddress;
      this.channel = channel;
      this.execController = execController;
      this.useSsl = useSsl;
      this.applicationState = applicationState;
    }
  }

  private static final class ApplicationState {
    @Nullable
    private DefinitionBuild definitionBuild;
    @Nullable
    private Registry serverRegistry;
    @Nullable
    private ServicesGraph servicesGraph;
  }

  private static final class ReloadingState {
    private boolean reloading;
  }

  private RunningState runningState;

  private final ReloadingState reloadingState = new ReloadingState();

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

    ApplicationState applicationState = new ApplicationState();

    LOGGER.info("Starting server...");

    applicationState.definitionBuild = buildUserDefinition();
    if (applicationState.definitionBuild.error != null) {
      if (applicationState.definitionBuild.definition.serverConfig.isDevelopment()) {
        LOGGER.warn("Exception raised getting server config (will use default config until reload):", applicationState.definitionBuild.error);
      } else {
        throw Exceptions.toException(applicationState.definitionBuild.error);
      }
    }

    ServerConfig serverConfig = applicationState.definitionBuild.definition.serverConfig;
    ExecControllerInternal execController = new DefaultExecController(serverConfig.getThreads());
    try {
      ChannelHandler channelHandler = ExecThreadBinding.bindFor(true, execController, () ->
        buildHandler(
          this,
          applicationState,
          impositions,
          reloadingState,
          execController,
          this::buildUserDefinition
        )
      );
      Channel channel = buildChannel(serverConfig, channelHandler, execController);
      InetSocketAddress boundAddress = (InetSocketAddress) channel.localAddress();

      // App is now considered running
      runningState = new RunningState(
        boundAddress,
        channel,
        execController,
        serverConfig.getSslContext() != null || serverConfig.getSniSslContext() != null,
        applicationState
      );
    } catch (Throwable e) {
      execController.close();
      throw e;
    }

    postStart();
  }

  private void postStart() throws Exception {
    ServerConfig serverConfig = runningState.applicationState.definitionBuild.definition.serverConfig;
    try {
      String startMessage = String.format("Ratpack started %sfor %s://%s:%s", serverConfig.isDevelopment() ? "(development) " : "", getScheme(), getBindHost(), getBindPort());

      if (serverConfig.getPortFile().isPresent()) {
        final Path portFilePath = serverConfig.getPortFile().get();
        try (FileOutputStream fout = new FileOutputStream(portFilePath.toFile())) {
          fout.write(Integer.toString(getBindPort()).getBytes());
        }
      }

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
            } catch (Exception e) {
              e.printStackTrace(System.err);
            }
          }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
      }
    } catch (Throwable e) {
      stop();
      throw e;
    }
  }

  private static final class DefinitionBuild {
    private final RatpackServerDefinition definition;
    private final Throwable error;
    private final Function<? super Registry, ? extends Registry> userRegistryFactory;

    DefinitionBuild(Impositions impositions, RatpackServerDefinition definition, Throwable error) {
      this.definition = definition;
      this.error = error;

      this.userRegistryFactory = baseRegistry -> {
        Registry userRegistry = definition.registry.apply(baseRegistry);
        for (UserRegistryImposition userRegistryImposition : impositions.getAll(UserRegistryImposition.class)) {
          userRegistry = userRegistry.join(userRegistryImposition.build(userRegistry));
        }

        return userRegistry;
      };
    }

  }

  private DefinitionBuild buildUserDefinition() throws Exception {
    return Impositions.impose(impositions, () -> {
      try {
        return new DefinitionBuild(impositions, RatpackServerDefinition.build(definitionFactory), null);
      } catch (Exception e) {
        return new DefinitionBuild(impositions, RatpackServerDefinition.build(s -> s.handler(r -> ctx -> ctx.error(e))), e);
      }
    });
  }

  private static ChannelHandler buildHandler(
    RatpackServer server,
    ApplicationState applicationState,
    Impositions impositions,
    ReloadingState reloadingState,
    ExecControllerInternal execController,
    Factory<DefinitionBuild> definitionBuilder
  ) throws Exception {
    if (applicationState.definitionBuild.definition.serverConfig.isDevelopment()) {
      return new ReloadHandler(server, applicationState, reloadingState, impositions, execController, definitionBuilder);
    } else {
      return buildAdapter(server, applicationState, reloadingState, impositions, execController);
    }
  }

  private static Channel buildChannel(ServerConfig serverConfig, ChannelHandler handlerAdapter, ExecController execController) throws InterruptedException {

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
      .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
      .childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
      .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) {
          ChannelPipeline pipeline = ch.pipeline();

          new ConnectionIdleTimeout(pipeline, serverConfig.getIdleTimeout());

          Mapping<String, SslContext> sniSslContext = serverConfig.getSniSslContext();
          if (sniSslContext != null) {
            pipeline.addLast("sniSsl", new SniHandler(sniSslContext));
          } else {
            SslContext sslContext = serverConfig.getSslContext();
            if (sslContext != null) {
              pipeline.addLast("ssl", sslContext.newHandler(ByteBufAllocator.DEFAULT));
            }
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

  private static NettyHandlerAdapter buildAdapter(
    RatpackServer server,
    ApplicationState applicationState,
    ReloadingState reloadingState,
    Impositions impositions,
    ExecControllerInternal execController
  ) throws Exception {
    LOGGER.info("Building registry...");
    applicationState.serverRegistry = buildServerRegistry(
      applicationState.definitionBuild.definition.serverConfig,
      applicationState.definitionBuild.userRegistryFactory, server, impositions, execController
    );

    Handler ratpackHandler = buildRatpackHandler(
      applicationState.serverRegistry,
      applicationState.definitionBuild.definition.handler
    );

    ratpackHandler = decorateHandler(ratpackHandler, applicationState.serverRegistry);

    applicationState.servicesGraph = new ServicesGraph(applicationState.serverRegistry);
    applicationState.servicesGraph.start(new DefaultEvent(applicationState.serverRegistry, reloadingState.reloading));
    return new NettyHandlerAdapter(applicationState.serverRegistry, ratpackHandler);
  }

  private static Registry buildServerRegistry(
    ServerConfig serverConfig,
    Function<? super Registry, ? extends Registry> userRegistryFactory,
    RatpackServer ratpackServer,
    Impositions impositions,
    ExecControllerInternal execController
  ) {
    return ServerRegistry.serverRegistry(ratpackServer, impositions, execController, serverConfig, userRegistryFactory);
  }

  private static Handler decorateHandler(Handler rootHandler, Registry serverRegistry) throws Exception {
    final Iterable<? extends HandlerDecorator> all = serverRegistry.getAll(HANDLER_DECORATOR_TYPE_TOKEN);
    for (HandlerDecorator handlerDecorator : all) {
      rootHandler = handlerDecorator.decorate(serverRegistry, rootHandler);
    }
    return rootHandler;
  }

  private static Handler buildRatpackHandler(Registry serverRegistry, Function<? super Registry, ? extends Handler> handlerFactory) throws Exception {
    return handlerFactory.apply(serverRegistry);
  }

  @Override
  public synchronized void stop() throws Exception {
    if (!isRunning()) {
      return;
    }
    RunningState runningState = this.runningState;
    this.runningState = null;

    try {
      if (shutdownHookThread != null) {
        Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
      }
    } catch (Exception ignored) {
      // just ignore
    }

    LOGGER.info("Stopping server...");

    try {
      if (runningState.channel != null) {
        runningState.channel.close().sync();
      }

      if (runningState.execController != null) {
        try {
          shutdownServices(runningState.applicationState, reloadingState);
        } finally {
          runningState.execController.close();
        }
      }

      LOGGER.info("Server stopped.");
    } finally {
      runningState.stopLatch.countDown();
    }
  }

  @Override
  public boolean await(Duration timeout) throws InterruptedException {
    RunningState runningState = this.runningState;
    if (runningState == null) {
      return true;
    } else {
      return runningState.stopLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
  }

  private void shutdownServices(ApplicationState applicationState, ReloadingState reloadingState) throws Exception {
    if (applicationState.servicesGraph != null) {
      applicationState.servicesGraph.stop(new DefaultEvent(applicationState.serverRegistry, reloadingState.reloading));
    }
  }

  @Override
  public synchronized RatpackServer reload() throws Exception {
    reloadingState.reloading = true;
    boolean start = false;

    if (this.isRunning()) {
      start = true;
      this.stop();
    }

    if (start) {
      start();
    }

    reloadingState.reloading = false;
    return this;
  }

  @Override
  public Optional<Registry> getRegistry() {
    return Optional.of(runningState)
      .map(r -> r.applicationState)
      .map(a -> a.serverRegistry);
  }

  @Override
  public synchronized boolean isRunning() {
    return runningState != null;
  }

  @Override
  public synchronized String getScheme() {
    return isRunning() ? this.runningState.useSsl ? "https" : "http" : null;
  }

  public synchronized int getBindPort() {
    return runningState == null || runningState.boundAddress == null ? -1 : runningState.boundAddress.getPort();
  }

  public synchronized String getBindHost() {
    if (runningState == null || runningState.boundAddress == null) {
      return null;
    } else {
      return HostUtil.determineHost(runningState.boundAddress);
    }
  }

  private static InetSocketAddress buildSocketAddress(ServerConfig serverConfig) {
    return (serverConfig.getAddress() == null) ? new InetSocketAddress(serverConfig.getPort()) : new InetSocketAddress(serverConfig.getAddress(), serverConfig.getPort());
  }


  @ChannelHandler.Sharable
  private final static class ReloadHandler extends ChannelInboundHandlerAdapter {

    private ServerConfig lastServerConfig;

    private final RatpackServer server;
    private final ApplicationState applicationState;
    private final ReloadingState reloadingState;
    private final Impositions impositions;
    private final ExecControllerInternal execController;
    private final Throttle reloadThrottle = Throttle.ofSize(1);
    private final Factory<DefinitionBuild> definitionBuilder;

    private ChannelInboundHandlerAdapter inner;

    public ReloadHandler(
      RatpackServer server,
      ApplicationState applicationState,
      ReloadingState reloadingState,
      Impositions impositions,
      ExecControllerInternal execController,
      Factory<DefinitionBuild> definitionBuilder
    ) {
      this.server = server;
      this.applicationState = applicationState;
      this.reloadingState = reloadingState;
      this.impositions = impositions;
      this.execController = execController;
      this.definitionBuilder = definitionBuilder;
      this.lastServerConfig = applicationState.definitionBuild.definition.serverConfig;
      try {
        this.inner = buildAdapter(server, applicationState, reloadingState, impositions, execController);
      } catch (Exception e) {
        LOGGER.warn("An error occurred during startup. This will be ignored due to being in development mode.", e);
        this.inner = null;
        applicationState.serverRegistry = buildServerRegistry(lastServerConfig, r -> r, server, impositions, execController);
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
        ctx.channel().attr(CHANNEL_THROTTLE_KEY).set(requestThrottle);
      } else {
        requestThrottle = ctx.channel().attr(CHANNEL_THROTTLE_KEY).get();
      }

      execController.fork().eventLoop(ctx.channel().eventLoop()).start(e ->
        Promise.<ChannelHandler>async(f -> {
            boolean rebuild = false;

            if (inner == null || applicationState.definitionBuild.error != null) {
              rebuild = true;
            } else if (msg instanceof HttpRequest) {
              Optional<ReloadInformant> reloadInformant = applicationState.serverRegistry.first(RELOAD_INFORMANT_TYPE, r -> r.shouldReload(applicationState.serverRegistry) ? r : null);
              if (reloadInformant.isPresent()) {
                LOGGER.debug("reload requested by '" + reloadInformant.get() + "'");
                rebuild = true;
              }
            }

            if (rebuild) {
              Blocking.get(() -> {
                  applicationState.definitionBuild = definitionBuilder.create();
                  lastServerConfig = applicationState.definitionBuild.definition.serverConfig;
                  return buildAdapter(server, applicationState, reloadingState, impositions, execController);
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
        return new NettyHandlerAdapter(applicationState.serverRegistry, context -> context.error(e));
      } catch (Exception e1) {
        throw uncheck(e);
      }
    }

  }

}
