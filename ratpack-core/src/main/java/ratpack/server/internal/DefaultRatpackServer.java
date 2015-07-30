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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Blocking;
import ratpack.exec.ExecController;
import ratpack.exec.Promise;
import ratpack.exec.Throttle;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.internal.FactoryHandler;
import ratpack.registry.Registry;
import ratpack.reload.internal.ClassUtil;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.server.*;
import ratpack.util.Exceptions;
import ratpack.util.internal.ChannelImplDetector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static ratpack.util.Exceptions.uncheck;

public class DefaultRatpackServer implements RatpackServer {

  static {
    if (System.getProperty("io.netty.leakDetectionLevel", null) == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }
  }

  public static final TypeToken<HandlerDecorator> HANDLER_DECORATOR_TYPE_TOKEN = TypeToken.of(HandlerDecorator.class);
  private static final Logger LOGGER = LoggerFactory.getLogger(RatpackServer.class);

  protected final Action<? super RatpackServerSpec> definitionFactory;

  protected InetSocketAddress boundAddress;
  protected Channel channel;
  protected DefaultExecController execController;
  protected Registry serverRegistry = Registry.empty();

  protected boolean reloading;
  protected final AtomicBoolean needsReload = new AtomicBoolean();

  protected boolean useSsl;
  private final ServerCapturer.Overrides overrides;

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

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(String.format("Ratpack started for %s://%s:%s", getScheme(), getBindHost(), getBindPort()));
    }
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

    SSLContext sslContext = serverConfig.getSSLContext();
    boolean requireClientSslAuth = serverConfig.isRequireClientSslAuth();
    this.useSsl = sslContext != null;

    ServerBootstrap serverBootstrap = new ServerBootstrap();

    serverConfig.getConnectTimeoutMillis().ifPresent(i -> {
      serverBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, i);
      serverBootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, i);
    });
    serverConfig.getMaxMessagesPerRead().ifPresent(i -> {
      serverBootstrap.option(ChannelOption.MAX_MESSAGES_PER_READ, i);
      serverBootstrap.childOption(ChannelOption.MAX_MESSAGES_PER_READ, i);
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
      .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline pipeline = ch.pipeline();
          if (sslContext != null) {
            SSLEngine sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(requireClientSslAuth);
            pipeline.addLast("ssl", new SslHandler(sslEngine));
          }

          pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false));
          pipeline.addLast("encoder", new HttpResponseEncoder());
          pipeline.addLast("aggregator", new HttpObjectAggregator(serverConfig.getMaxContentLength()));
          pipeline.addLast("deflater", new SmartHttpContentCompressor());
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

    Iterable<? extends Service> services = Sets.newLinkedHashSet(serverRegistry.getAll(Service.class));
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
      shutdownServices();
    } finally {
      Optional.ofNullable(channel).ifPresent(Channel::close);
      Optional.ofNullable(execController).ifPresent(ExecController::close);
      channel = null;
      execController = null;
    }
  }

  private void shutdownServices() throws Exception {
    if (serverRegistry != null) {
      Iterable<? extends Service> services = Sets.newLinkedHashSet(serverRegistry.getAll(Service.class));
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
      execController.exec()
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
  private class ReloadHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
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
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
      execController.exec().start(e ->
          Promise.<ChannelHandler>of(f -> {
            boolean rebuild = false;

            if (inner == null || definitionBuild.error != null) {
              rebuild = true;
            } else {
              Optional<ReloadInformant> reloadInformant = serverRegistry.first(TypeToken.of(ReloadInformant.class), r -> r.shouldReload(serverRegistry) ? r : null);
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
            .throttled(reloadThrottle)
            .then(adapter -> Blocking.exec(() -> delegate(ctx, adapter, msg)))
      );
    }

    private NettyHandlerAdapter buildErrorRenderingAdapter(Throwable e) {
      try {
        return new NettyHandlerAdapter(serverRegistry, context -> context.error(e));
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
