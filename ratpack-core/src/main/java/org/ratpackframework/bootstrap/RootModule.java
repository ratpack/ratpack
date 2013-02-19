package org.ratpackframework.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.ratpackframework.bootstrap.internal.NettyRatpackServer;
import org.ratpackframework.bootstrap.internal.NoopInit;
import org.ratpackframework.error.DefaultErrorHandler;
import org.ratpackframework.error.DefaultNotFoundHandler;
import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.handler.NettyRoutingAdapter;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.CompositeRouter;
import org.ratpackframework.routing.Routed;
import org.ratpackframework.templating.NullTemplateRenderer;
import org.ratpackframework.templating.TemplateRenderer;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RootModule extends AbstractModule {

  public static final String MAIN_APP_HTTP_HANDLER = "mainAppHttpHandler";
  public static final String MAIN_STATIC_ASSET_HTTP_HANDLER = "mainStaticAssetHttpHandler";

  public static final String MAIN_HTTP_ERROR_HANDLER = "mainHttpErrorHandler";
  public static final String MAIN_NOT_FOUND_HTTP_HANDLER = "mainNotFoundHttpHandler";

  public static final String MAIN_HTTP_HANDLER = "mainHttpHandler";
  public static final String MAIN_HTTP_HANDLER_PIPELINE = "mainHttpHandlerPipeline";

  public static final String WORKER_EXECUTOR = "workerExecutor";
  public static final String BOSS_EXECUTOR = "bossExecutor";

  public static final String NETTY_ROUTING_ADAPTER = "nettyRoutingHandler";

  public static final TypeLiteral<Handler<Routed<HttpExchange>>> HTTP_HANDLER = new TypeLiteral<Handler<Routed<HttpExchange>>>() {
  };
  public static final TypeLiteral<Handler<ErroredHttpExchange>> HTTP_ERROR_HANDLER = new TypeLiteral<Handler<ErroredHttpExchange>>() {
  };
  public static final TypeLiteral<List<Handler<Routed<HttpExchange>>>> HTTP_HANDLER_PIPELINE = new TypeLiteral<List<Handler<Routed<HttpExchange>>>>() {
  };
  public static final TypeLiteral<Handler<RatpackServer>> RATPACK_INIT = new TypeLiteral<Handler<RatpackServer>>() {
  };
  public static final TypeLiteral<List<String>> STRING_LIST = new TypeLiteral<List<String>>() {
  };
  public static final String BASE_DIR = "baseDir";
  public static final String BIND_PORT = "bindPort";
  public static final String BIND_HOST = "bindHost";
  public static final String BIND_ADDRESS = "bindAddress";
  public static final String PUBLIC_HOST = "publicHost";

  private final File baseDir;
  private final int bindPort;
  private final String bindHost;
  private final String publicHost;

  public RootModule(File baseDir, int bindPort, String bindHost, String publicHost) {
    this.baseDir = baseDir;
    this.bindPort = bindPort;
    this.bindHost = bindHost;
    this.publicHost = publicHost;
  }

  @Override
  protected void configure() {
    bind(HTTP_ERROR_HANDLER).annotatedWith(Names.named(MAIN_HTTP_ERROR_HANDLER)).to(DefaultErrorHandler.class);
    bind(HTTP_HANDLER).annotatedWith(Names.named(MAIN_NOT_FOUND_HTTP_HANDLER)).to(DefaultNotFoundHandler.class);

    bind(TemplateRenderer.class).to(NullTemplateRenderer.class).in(Singleton.class);

    bind(SimpleChannelUpstreamHandler.class).annotatedWith(Names.named(NETTY_ROUTING_ADAPTER)).to(NettyRoutingAdapter.class);

    bind(File.class).annotatedWith(Names.named(BASE_DIR)).toInstance(baseDir);
    bind(Integer.class).annotatedWith(Names.named(BIND_PORT)).toInstance(bindPort);

    LinkedBindingBuilder<String> bindHostBinding = bind(String.class).annotatedWith(Names.named(BIND_HOST));
    if (bindHost == null) {
      bindHostBinding.toProvider(Providers.<String>of(null));
    } else {
      bindHostBinding.toInstance(bindHost);
    }

    LinkedBindingBuilder<String> publicHostBinding = bind(String.class).annotatedWith(Names.named(PUBLIC_HOST));
    if (publicHost == null) {
      publicHostBinding.toProvider(Providers.<String>of(null));
    } else {
      publicHostBinding.toInstance(publicHost);
    }

    bind(RATPACK_INIT).to(NoopInit.class);
    bind(RatpackServer.class).to(NettyRatpackServer.class);
  }

  @Provides
  @Named(MAIN_HTTP_HANDLER_PIPELINE)
  @Inject(optional = true)
  List<Handler<Routed<HttpExchange>>> provideMainHttpHandlerPipeline(
      @Named(MAIN_APP_HTTP_HANDLER) Handler<Routed<HttpExchange>> appHandler,
      @Named(MAIN_STATIC_ASSET_HTTP_HANDLER) Handler<Routed<HttpExchange>> staticHandler,
      @Named(MAIN_NOT_FOUND_HTTP_HANDLER) Handler<Routed<HttpExchange>> notFoundHandler
  ) {
    List<Handler<Routed<HttpExchange>>> handlers = new ArrayList<>(3);
    if (appHandler != null) {
      handlers.add(appHandler);
    }
    if (staticHandler != null) {
      handlers.add(staticHandler);
    }
    handlers.add(notFoundHandler);

    return handlers;
  }

  @Provides
  @Named(MAIN_HTTP_HANDLER)
  Handler<Routed<HttpExchange>> provideMainHttpHandler(@Named(MAIN_HTTP_HANDLER_PIPELINE) List<Handler<Routed<HttpExchange>>> pipeline) {
    return new CompositeRouter<>(pipeline);
  }

  @Provides
  @Singleton
  ChannelGroup provideChannelGroup() {
    return new DefaultChannelGroup("http-server");
  }

  @Provides
  ChannelFactory provideChannelFactory(@Named(BOSS_EXECUTOR) Executor bossExecutor, @Named(WORKER_EXECUTOR) Executor workerExecutor) {
    return new NioServerSocketChannelFactory(bossExecutor, workerExecutor);
  }

  @Provides
  @Singleton
  @Named(WORKER_EXECUTOR)
  Executor provideWorkerExecutor() {
    return Executors.newCachedThreadPool();
  }

  @Provides
  @Singleton
  @Named(BOSS_EXECUTOR)
  Executor provideBossExecutor() {
    return Executors.newCachedThreadPool();
  }

  @Provides
  ChannelPipelineFactory provideChannelPipelineFactory(@Named(NETTY_ROUTING_ADAPTER) final SimpleChannelUpstreamHandler appHandler) {
    return new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        return Channels.pipeline(new HttpRequestDecoder(), new HttpResponseEncoder(), new HttpContentCompressor(), appHandler);
      }
    };
  }

  @Provides
  @Named(BIND_ADDRESS)
  InetSocketAddress provideBindAddress() {
    InetSocketAddress address;
    if (bindHost == null) {
      address = new InetSocketAddress(bindPort);
    } else {
      address = new InetSocketAddress(bindHost, bindPort);
    }
    return address;
  }
}
