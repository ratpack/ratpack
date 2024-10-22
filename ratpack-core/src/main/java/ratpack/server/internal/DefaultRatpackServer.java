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

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
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
import ratpack.config.ConfigObject;
import ratpack.exec.*;
import ratpack.exec.internal.ExecThreadBinding;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.impose.Impositions;
import ratpack.impose.UserRegistryImposition;
import ratpack.registry.Registry;
import ratpack.server.*;
import ratpack.service.internal.DefaultEvent;
import ratpack.service.internal.ServicesGraph;
import ratpack.util.Exceptions;
import ratpack.util.Types;
import ratpack.util.internal.TransportDetector;

import javax.net.ssl.SSLEngine;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ratpack.util.Exceptions.uncheck;

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
    private final ExecController execController;
    private final boolean useSsl;
    private final DefaultRatpackServer.ApplicationState applicationState;
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    private final boolean inheritedExecController;

    RunningState(
        InetSocketAddress boundAddress,
        Channel channel,
        ExecController execController,
        boolean inheritedExecController,
        boolean useSsl,
        DefaultRatpackServer.ApplicationState applicationState
    ) {
      this.boundAddress = boundAddress;
      this.channel = channel;
      this.execController = execController;
      this.inheritedExecController = inheritedExecController;
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

  @Nullable
  private volatile RunningState runningState;

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
    RunningState runningState;
    if (isRunning()) {
      return;
    }

    ApplicationState applicationState = new ApplicationState();

    LOGGER.info("Starting server...");

    applicationState.definitionBuild = buildUserDefinition();
    RatpackServerDefinition definition = applicationState.definitionBuild.definition;
    if (applicationState.definitionBuild.error != null) {
      if (definition.serverConfig.isDevelopment()) {
        LOGGER.warn("Exception raised getting server config (will use default config until reload):", applicationState.definitionBuild.error);
      } else {
        throw Exceptions.toException(applicationState.definitionBuild.error);
      }
    }

    ServerConfig serverConfig = definition.serverConfig;
    boolean inheritedExecController = serverConfig.getInheritedExecController().isPresent();
    ExecController execController = serverConfig.getInheritedExecController()
        .orElseGet(() -> ExecController.builder()
            .numThreads(serverConfig.getThreads())
            .build()
        );
    try {
      ChannelHandler channelHandler = ExecThreadBinding.bindFor(true, execController, () ->
          buildHandler(
              this,
              applicationState,
              impositions,
              reloadingState,
              execController,
              inheritedExecController,
              this::buildUserDefinition
          )
      );
      Channel channel = buildChannel(serverConfig, channelHandler, execController);
      InetSocketAddress boundAddress = (InetSocketAddress) channel.localAddress();

      // App is now considered running
      this.runningState = runningState = new RunningState(
          boundAddress,
          channel,
          execController,
          inheritedExecController,
          serverConfig.getNettySslContext() != null,
          applicationState
      );
    } catch (Throwable e) {
      execController.close();
      throw e;
    }

    postStart(runningState);
  }

  private void postStart(RunningState runningState) throws Exception {
    ServerConfig serverConfig = Objects.requireNonNull(runningState.applicationState.definitionBuild).definition.serverConfig;
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
    return impositions.imposeOver(() -> {
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
      ExecController execController,
      boolean inheritedExecController,
      Factory<DefinitionBuild> definitionBuilder
  ) throws Exception {
    if (Objects.requireNonNull(applicationState.definitionBuild).definition.serverConfig.isDevelopment()) {
      return new ReloadHandler(server, applicationState, reloadingState, impositions, execController, inheritedExecController, definitionBuilder);
    } else {
      return buildAdapter(server, applicationState, reloadingState, impositions, execController, inheritedExecController);
    }
  }

  private static Channel buildChannel(ServerConfig serverConfig, ChannelHandler handlerAdapter, ExecController execController) throws InterruptedException {

    SslContext sslContext = serverConfig.getNettySslContext();

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
    serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, serverConfig.isTcpKeepAlive());

    serverBootstrap
        .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
        .childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);

    applyServerChildOptions(serverConfig, serverBootstrap);

    return serverBootstrap
        .group(execController.getEventLoopGroup())
        .channel(TransportDetector.getServerSocketChannelImpl())
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();

            if (sslContext != null) {
              SSLEngine sslEngine = sslContext.newEngine(ByteBufAllocator.DEFAULT);
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

  private static void applyServerChildOptions(ServerConfig serverConfig, ServerBootstrap serverBootstrap) {
    for (ConfigObject<?> configObject : serverConfig.getRequiredConfig()) {
      if (ServerChannelOptions.class.isAssignableFrom(configObject.getType())) {
        ServerChannelOptions serverChannelOptions = (ServerChannelOptions) configObject.getObject();
        serverChannelOptions.setOptions(new ServerChannelOptions.OptionSetter() {
          @Override
          public <T> ServerChannelOptions.OptionSetter set(ChannelOption<T> option, T value) {
            serverBootstrap.option(option, value);
            return this;
          }
        });
        serverChannelOptions.setChildOptions(new ServerChannelOptions.OptionSetter() {
          @Override
          public <T> ServerChannelOptions.OptionSetter set(ChannelOption<T> option, T value) {
            serverBootstrap.childOption(option, value);
            return this;
          }
        });
      }
    }
  }


  private static NettyHandlerAdapter buildAdapter(
      RatpackServer server,
      ApplicationState applicationState,
      ReloadingState reloadingState,
      Impositions impositions,
      ExecController execController,
      boolean inheritedExecController
  ) throws Exception {
    LOGGER.info("Building registry...");
    applicationState.serverRegistry = buildServerRegistry(
        Objects.requireNonNull(applicationState.definitionBuild).definition.serverConfig,
        applicationState.definitionBuild.userRegistryFactory, server, impositions, execController
    );

    if (!inheritedExecController) {
      execController.addInterceptors(ImmutableList.copyOf(applicationState.serverRegistry.getAll(ExecInterceptor.class)));
      execController.addInitializers(ImmutableList.copyOf(applicationState.serverRegistry.getAll(ExecInitializer.class)));
    }

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
      ExecController execController
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
    RunningState runningState = this.runningState;
    if (runningState == null) {
      return;
    }
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

      try {
        shutdownServices(runningState.applicationState, reloadingState);
      } finally {
        if (!runningState.inheritedExecController) {
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
    return Optional.ofNullable(runningState)
        .map(r -> r.applicationState)
        .map(a -> a.serverRegistry);
  }

  @Override
  public synchronized boolean isRunning() {
    return runningState != null;
  }

  @Override
  public synchronized String getScheme() {
    return Optional.ofNullable(this.runningState)
        .map(r -> r.useSsl ? "https" : "http")
        .orElse(null);
  }

  public synchronized int getBindPort() {
    return Optional.ofNullable(this.runningState)
        .map(r -> r.boundAddress.getPort())
        .orElse(-1);
  }

  public synchronized String getBindHost() {
    return Optional.ofNullable(this.runningState)
        .map(r -> HostUtil.determineHost(r.boundAddress))
        .orElse(null);
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
    private final ExecController execController;
    private final Throttle reloadThrottle = Throttle.ofSize(1);
    private final boolean inheritedExecController;
    private final Factory<DefinitionBuild> definitionBuilder;

    private ChannelInboundHandlerAdapter inner;

    public ReloadHandler(
        RatpackServer server,
        ApplicationState applicationState,
        ReloadingState reloadingState,
        Impositions impositions,
        ExecController execController,
        boolean inheritedExecController,
        Factory<DefinitionBuild> definitionBuilder
    ) {
      this.server = server;
      this.applicationState = applicationState;
      this.reloadingState = reloadingState;
      this.impositions = impositions;
      this.execController = execController;
      this.inheritedExecController = inheritedExecController;
      this.definitionBuilder = definitionBuilder;
      this.lastServerConfig = Objects.requireNonNull(applicationState.definitionBuild).definition.serverConfig;
      try {
        this.inner = buildAdapter(server, applicationState, reloadingState, impositions, execController, inheritedExecController);
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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
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

                if (inner == null || Objects.requireNonNull(applicationState.definitionBuild).error != null) {
                  rebuild = true;
                } else if (msg instanceof HttpRequest) {
                  Optional<ReloadInformant> reloadInformant;
                  try {
                    reloadInformant = applicationState.serverRegistry.first(RELOAD_INFORMANT_TYPE, r -> r.shouldReload(applicationState.serverRegistry) ? r : null);
                  } catch (Exception ex) {
                    f.error(ex);
                    return;
                  }
                  if (reloadInformant.isPresent()) {
                    LOGGER.debug("reload requested by '" + reloadInformant.get() + "'");
                    rebuild = true;
                  }
                }

                if (rebuild) {
                  Blocking.get(() -> {
                        applicationState.definitionBuild = definitionBuilder.create();
                        lastServerConfig = applicationState.definitionBuild.definition.serverConfig;
                        return buildAdapter(server, applicationState, reloadingState, impositions, execController, inheritedExecController);
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
