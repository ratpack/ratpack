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
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import ratpack.exec.Throttle;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.registry.Registry;
import ratpack.server.*;
import ratpack.util.Exceptions;
import ratpack.util.Types;
import ratpack.util.internal.ChannelImplDetector;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static ratpack.util.Exceptions.uncheck;

public class DefaultRatpackServer implements RatpackServer {

  public static final TypeToken<ReloadInformant> RELOAD_INFORMANT_TYPE = Types.token(ReloadInformant.class);

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

  protected boolean reloading;
  protected final AtomicBoolean needsReload = new AtomicBoolean();

  protected boolean useSsl;
  private final ServerCapturer.Overrides overrides;
  private Thread shutdownHookThread;

  public DefaultRatpackServer(Action<? super RatpackServerSpec> definitionFactory) throws Exception {
    this.definitionFactory = definitionFactory;
    this.overrides = ServerCapturer.capture(this);
  }

  @Override
  public synchronized void start() throws Exception {
    if (isRunning()) {
      return;
    }

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
    ChannelHandler channelHandler = buildHandler(definitionBuild);
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

  private static class DefinitionBuild {
    private final ServerCapturer.Overrides overrides;
    private final RatpackServerDefinition definition;
    private final Throwable error;
    private final ServerConfig serverConfig;
    private final Function<? super Registry, ? extends Registry> userRegistryFactory;

    public DefinitionBuild(ServerCapturer.Overrides overrides, RatpackServerDefinition definition, Throwable error) {
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

      Registry serverConfigOverrideRegistry = Registry.single(ServerConfig.class, serverConfig);
      this.userRegistryFactory = baseRegistry -> {
        Registry actualBaseRegistry = baseRegistry.join(serverConfigOverrideRegistry);
        Registry userRegistry = definition.getRegistry().apply(actualBaseRegistry);
        Registry overrideRegistry = overrides.getRegistryFunction().apply(userRegistry);
        return userRegistry.join(overrideRegistry);
      };
    }

    public ServerCapturer.Overrides getOverrides() {
      return overrides;
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
    try {
      return new DefinitionBuild(overrides, RatpackServerDefinition.build(definitionFactory), null);
    } catch (Exception e) {
      return new DefinitionBuild(overrides, RatpackServerDefinition.build(s -> s.handler(r -> ctx -> ctx.error(e))), e);
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
    SslContext sslCtx = serverConfig.getSsl();

    // Just for backward compatibility
    if (sslCtx == null) {
      SSLContext sslContext = serverConfig.getSslContext();
      this.useSsl = sslContext != null;
    } else {
      this.useSsl = true;
    }

    boolean requireClientSslAuth = serverConfig.isRequireClientSslAuth();
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

    return serverBootstrap
      .group(execController.getEventLoopGroup())
      .channel(ChannelImplDetector.getServerSocketChannelImpl())
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .childHandler(new NettyChannelInitializer(handlerAdapter, serverConfig))
      .bind(buildSocketAddress(serverConfig))
      .sync()
      .channel();
  }

  protected NettyHandlerAdapter buildAdapter(DefinitionBuild definition) throws Exception {
    LOGGER.info("Building registry...");
    serverRegistry = buildServerRegistry(definition.getServerConfig(), definition.getUserRegistryFactory());

    Handler ratpackHandler = buildRatpackHandler(serverRegistry, definition.getHandlerFactory());
    ratpackHandler = decorateHandler(ratpackHandler, serverRegistry);

    Set<? extends Service> services = Sets.newLinkedHashSet(serverRegistry.getAll(Service.class));
    if (!services.isEmpty()) {

      LOGGER.info("Initializing " + services.size() + " services...");
      try {
        executeEvents(services.iterator(), new DefaultEvent(serverRegistry, reloading), Service::onStart, (service, error) -> {
          throw new StartupFailureException("Service '" + service.getName() + "' failed startup", error);
        });
      } catch (StartupFailureException e) {
        try {
          shutdownServices();
        } catch (Exception e1) {
          e.addSuppressed(e1);
        }
        throw e;
      }
    }

    return new NettyHandlerAdapter(serverRegistry, ratpackHandler);
  }

  private Registry buildServerRegistry(ServerConfig serverConfig, Function<? super Registry, ? extends Registry> userRegistryFactory) {
    return ServerRegistry.serverRegistry(this, execController, serverConfig, userRegistryFactory);
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
    if (serverRegistry != null) {
      Set<? extends Service> services = Sets.newLinkedHashSet(serverRegistry.getAll(Service.class));
      if (services.isEmpty()) {
        return;
      }

      LOGGER.info("Stopping " + services.size() + " services...");
      Iterator<Service> reverseServices = ImmutableList.copyOf(services).reverse().iterator();
      executeEvents(reverseServices, new DefaultEvent(serverRegistry, reloading), Service::onStop, (service, error) ->
          LOGGER.warn("Service '" + service.getName() + "' thrown an exception while stopping.", error)
      );
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

  private <E> void executeEvents(Iterator<? extends Service> services, E event, BiAction<Service, E> action, BiAction<? super Service, ? super Throwable> onError) throws Exception {
    if (!services.hasNext()) {
      return;
    }

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();
    executeEvents(services, latch, error, event, action, onError);
    latch.await();

    Throwable thrown = error.get();
    if (thrown != null) {
      throw Exceptions.toException(thrown);
    }
  }

  private <E> void executeEvents(Iterator<? extends Service> services, CountDownLatch latch, AtomicReference<Throwable> error, E event, BiAction<Service, E> action, BiAction<? super Service, ? super Throwable> onError) throws Exception {
    if (services.hasNext()) {
      Service service = services.next();
      execController.fork()
        .onError(t -> {
          try {
            onError.execute(service, t);
          } catch (Throwable e) {
            error.set(e);
          }
        })
        .onComplete(e -> {
          //noinspection ThrowableResultOfMethodCallIgnored
          if (error.get() == null) {
            executeEvents(services, latch, error, event, action, onError);
          } else {
            latch.countDown();
          }
        })
        .start(e -> action.execute(service, event));
    } else {
      latch.countDown();
    }
  }

  @ChannelHandler.Sharable
  class ReloadHandler extends SimpleChannelInboundHandler<HttpRequest> {
    private ServerConfig lastServerConfig;
    private DefinitionBuild definitionBuild;
    private final Throttle reloadThrottle = Throttle.ofSize(1);

    private ChannelHandler inner;

    public ReloadHandler(DefinitionBuild definition) {
      super(false);
      this.definitionBuild = definition;
      this.lastServerConfig = definitionBuild.getServerConfig();
      try {
        this.inner = buildAdapter(definitionBuild);
      } catch (Exception e) {
        this.inner = null;
        serverRegistry = buildServerRegistry(lastServerConfig, r -> r);
      }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      ctx.read();
      super.channelActive(ctx);
    }

    ChannelHandler getDelegate() {
      return inner;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
      execController.fork().eventLoop(ctx.channel().eventLoop()).start(e ->
          Promise.<ChannelHandler>of(f -> {
            boolean rebuild = false;

            if (inner == null || definitionBuild.error != null) {
              rebuild = true;
            } else {
              Optional<ReloadInformant> reloadInformant = serverRegistry.first(RELOAD_INFORMANT_TYPE, r -> r.shouldReload(serverRegistry) ? r : null);
              if (reloadInformant.isPresent()) {
                LOGGER.warn("reload requested by '" + reloadInformant.get() + "'");
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
            .then(adapter -> ctx.fireChannelRead(msg))
      );
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
