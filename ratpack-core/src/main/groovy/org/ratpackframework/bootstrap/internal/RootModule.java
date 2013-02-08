package org.ratpackframework.bootstrap.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.ratpackframework.Handler;
import org.ratpackframework.assets.DirectoryStaticAssetRequestHandler;
import org.ratpackframework.assets.FileStaticAssetRequestHandler;
import org.ratpackframework.assets.TargetFileStaticAssetRequestHandler;
import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.config.DeploymentConfig;
import org.ratpackframework.config.LayoutConfig;
import org.ratpackframework.config.StaticAssetsConfig;
import org.ratpackframework.groovy.routing.internal.ScriptBackedRouter;
import org.ratpackframework.groovy.templating.GroovyTemplateRenderer;
import org.ratpackframework.handler.HttpExchange;
import org.ratpackframework.handler.NotFoundHandler;
import org.ratpackframework.handler.RoutingHandler;
import org.ratpackframework.handler.internal.CompositeRoutingHandler;
import org.ratpackframework.routing.ResponseFactory;
import org.ratpackframework.routing.Routed;
import org.ratpackframework.routing.internal.DefaultResponseFactory;
import org.ratpackframework.session.DefaultSessionIdGenerator;
import org.ratpackframework.session.SessionConfig;
import org.ratpackframework.session.SessionIdGenerator;
import org.ratpackframework.session.SessionListener;
import org.ratpackframework.session.internal.DefaultSessionConfig;
import org.ratpackframework.session.internal.NoopSessionListener;
import org.ratpackframework.templating.TemplateRenderer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RootModule extends AbstractModule {

  private final TypeLiteral<Handler<Routed<HttpExchange>>> ROUTER = new TypeLiteral<Handler<Routed<HttpExchange>>>() {
  };
  private final TypeLiteral<List<Handler<Routed<HttpExchange>>>> ROUTING_PIPELINE = new TypeLiteral<List<Handler<Routed<HttpExchange>>>>() {
  };
  private TypeLiteral<Handler<RatpackServer>> RATPACK_INIT = new TypeLiteral<Handler<RatpackServer>>() {
  };

  @Override
  protected void configure() {
    bind(TemplateRenderer.class).to(GroovyTemplateRenderer.class).in(Singleton.class);
    bind(ResponseFactory.class).to(DefaultResponseFactory.class).in(Singleton.class);

    bind(SimpleChannelUpstreamHandler.class).annotatedWith(Names.named("nettyRoutingHandler")).to(RoutingHandler.class);
    bind(ROUTER).annotatedWith(Names.named("mainRouter")).toProvider(MainRouterProvider.class);
    bind(ROUTING_PIPELINE).annotatedWith(Names.named("mainRoutingPipeline")).toProvider(MainRoutingPipelineProvider.class);
    bind(ROUTER).annotatedWith(Names.named("applicationRouter")).to(ScriptBackedRouter.class);

    bind(ROUTER).annotatedWith(Names.named("staticAssetRouter")).toProvider(StaticAssetRouterProvider.class);
    bind(ROUTING_PIPELINE).annotatedWith(Names.named("mainStaticAssetRoutingPipeline")).toProvider(StaticAssetRoutingPipelineProvider.class);
    bind(ROUTER).annotatedWith(Names.named("targetFileStaticAssetHandler")).toProvider(TargetFileStaticAssetHandlerProvider.class);
    bind(ROUTER).annotatedWith(Names.named("directoryStaticAssetHandler")).to(DirectoryStaticAssetRequestHandler.class);
    bind(ROUTER).annotatedWith(Names.named("fileStaticAssetHandler")).to(FileStaticAssetRequestHandler.class);

    bind(SessionIdGenerator.class).to(DefaultSessionIdGenerator.class);
    bind(SessionListener.class).to(NoopSessionListener.class);
    bind(SessionConfig.class).to(DefaultSessionConfig.class);

    bind(ChannelFactory.class).toProvider(ChannelFactoryProvider.class);
    bind(ChannelPipelineFactory.class).toProvider(ChannelPipelineFactoryProvider.class);

    bind(InetSocketAddress.class).toProvider(InetSocketProvider.class);
    bind(RatpackServer.class).to(NettyRatpackServer.class);
    bind(RATPACK_INIT).to(NoopInit.class);
  }

  public static class MainRoutingPipelineProvider implements Provider<List<Handler<Routed<HttpExchange>>>> {
    @Inject
    @Named("applicationRouter")
    Handler<Routed<HttpExchange>> applicationRouter;
    @Inject
    @Named("staticAssetRouter")
    Handler<Routed<HttpExchange>> staticAssetRouter;

    @Inject
    NotFoundHandler notFoundHandler;

    @Override
    public List<Handler<Routed<HttpExchange>>> get() {
      return Arrays.asList(applicationRouter, staticAssetRouter, notFoundHandler);
    }
  }

  public static class MainRouterProvider implements Provider<Handler<Routed<HttpExchange>>> {
    @Inject
    @Named("mainRoutingPipeline")
    List<Handler<Routed<HttpExchange>>> pipeline;

    @Override
    public Handler<Routed<HttpExchange>> get() {
      //noinspection unchecked
      return new CompositeRoutingHandler(pipeline);
    }
  }

  public static class StaticAssetRouterProvider implements Provider<Handler<Routed<HttpExchange>>> {
    @Inject
    @Named("mainStaticAssetRoutingPipeline")
    List<Handler<Routed<HttpExchange>>> pipeline;

    @Override
    public Handler<Routed<HttpExchange>> get() {
      return new CompositeRoutingHandler<>(pipeline);
    }
  }

  static class TargetFileStaticAssetHandlerProvider implements Provider<Handler<Routed<HttpExchange>>> {
    @Inject
    LayoutConfig layoutConfig;

    @Inject
    StaticAssetsConfig staticAssetsConfig;

    @Override
    public Handler<Routed<HttpExchange>> get() {
      return new TargetFileStaticAssetRequestHandler(new File(layoutConfig.getBaseDir(), staticAssetsConfig.getDir()));
    }
  }

  public static class StaticAssetRoutingPipelineProvider implements Provider<List<Handler<Routed<HttpExchange>>>> {
    @Inject
    @Named("targetFileStaticAssetHandler")
    Handler<Routed<HttpExchange>> targetFile;

    @Inject
    @Named("directoryStaticAssetHandler")
    Handler<Routed<HttpExchange>> directory;

    @Inject
    @Named("fileStaticAssetHandler")
    Handler<Routed<HttpExchange>> file;

    @Override
    public List<Handler<Routed<HttpExchange>>> get() {
      return Arrays.asList(targetFile, directory, file);
    }
  }

  static class InetSocketProvider implements Provider<InetSocketAddress> {
    private final DeploymentConfig deploymentConfig;

    @Inject
    public InetSocketProvider(DeploymentConfig deploymentConfig) {
      this.deploymentConfig = deploymentConfig;
    }

    @Override
    public InetSocketAddress get() {
      if (deploymentConfig.getBindHost() == null) {
        return new InetSocketAddress(deploymentConfig.getPort());
      } else {
        return new InetSocketAddress(deploymentConfig.getBindHost(), deploymentConfig.getPort());
      }
    }
  }


  @Provides
  @Singleton
  ChannelGroup provideChannelGroup() {
    return new DefaultChannelGroup("http-server");
  }

  static class ChannelFactoryProvider implements Provider<ChannelFactory> {
    private final Executor bossExecutor;
    private final Executor workerExecutor;

    @Inject
    ChannelFactoryProvider(@Named("bossExecutor") Executor bossExecutor, @Named("workerExecutor") Executor workerExecutor) {
      this.bossExecutor = bossExecutor;
      this.workerExecutor = workerExecutor;
    }

    @Override
    public ChannelFactory get() {
      return new NioServerSocketChannelFactory(bossExecutor, workerExecutor);
    }
  }

  @Provides
  @Singleton
  @Named("workerExecutor")
  Executor provideWorkerExecutor() {
    return Executors.newCachedThreadPool();
  }

  @Provides
  @Singleton
  @Named("bossExecutor")
  Executor provideBossExecutor() {
    return Executors.newCachedThreadPool();
  }

  static class ChannelPipelineFactoryProvider implements Provider<ChannelPipelineFactory> {
    @Inject
    @Named("nettyRoutingHandler")
    SimpleChannelUpstreamHandler appHandler;

    @Override
    public ChannelPipelineFactory get() {
      return new ChannelPipelineFactory() {
        @Override
        public ChannelPipeline getPipeline() throws Exception {
          return Channels.pipeline(new HttpRequestDecoder(), new HttpResponseEncoder(), new HttpContentCompressor(), appHandler);
        }
      };
    }
  }

}
