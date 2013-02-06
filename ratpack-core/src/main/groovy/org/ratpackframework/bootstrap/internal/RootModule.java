package org.ratpackframework.bootstrap.internal;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.ratpackframework.assets.DirectoryStaticAssetRequestHandler;
import org.ratpackframework.assets.FileStaticAssetRequestHandler;
import org.ratpackframework.assets.HttpCachingStaticAssetRequestHandler;
import org.ratpackframework.assets.StaticAssetRequest;
import org.ratpackframework.bootstrap.RatpackApp;
import org.ratpackframework.config.DeploymentConfig;
import org.ratpackframework.config.LayoutConfig;
import org.ratpackframework.config.StaticAssetsConfig;
import org.ratpackframework.routing.ResponseFactory;
import org.ratpackframework.routing.RoutedRequest;
import org.ratpackframework.routing.internal.DefaultResponseFactory;
import org.ratpackframework.routing.internal.ScriptBackedRouter;
import org.ratpackframework.session.DefaultSessionIdGenerator;
import org.ratpackframework.session.SessionConfig;
import org.ratpackframework.session.SessionIdGenerator;
import org.ratpackframework.session.SessionListener;
import org.ratpackframework.session.internal.DefaultSessionConfig;
import org.ratpackframework.session.internal.NoopSessionListener;
import org.ratpackframework.templating.GroovyTemplateRenderer;
import org.ratpackframework.templating.TemplateRenderer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;

public class RootModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Vertx.class).toInstance(Vertx.newVertx());
    bind(HttpServer.class).toProvider(HttpServerProvider.class).in(Singleton.class);

    bind(TemplateRenderer.class).to(GroovyTemplateRenderer.class);

    bind(File.class).annotatedWith(Names.named("staticAssetDir")).toProvider(StaticAssetsDirProvider.class);
    bind(new TypeLiteral<Handler<StaticAssetRequest>>() {}).toProvider(StaticAssetHandlerProvider.class);

    bind(ResponseFactory.class).to(DefaultResponseFactory.class);
    bind(new TypeLiteral<Handler<RoutedRequest>>() {}).to(ScriptBackedRouter.class);

    bind(SessionIdGenerator.class).to(DefaultSessionIdGenerator.class);
    bind(SessionListener.class).to(NoopSessionListener.class);
    bind(SessionConfig.class).to(DefaultSessionConfig.class);

    bind(Integer.class).annotatedWith(Names.named("bindPort")).toProvider(BindingPortProvider.class);
    bind(String.class).annotatedWith(Names.named("bindHost")).toProvider(BindingHostProvider.class);
    bind(new TypeLiteral<Handler<RatpackApp>>() {}).to(NoopInit.class);

    bind(RatpackApp.class).to(DefaultRatpackApp.class);
  }


  public static class HttpServerProvider implements Provider<HttpServer> {
    private final Vertx vertx;
    @Inject
    public HttpServerProvider(Vertx vertx) {
      this.vertx = vertx;
    }
    @Override
    public HttpServer get() {
      return vertx.createHttpServer();
    }
  }

  public static class StaticAssetHandlerProvider implements Provider<Handler<StaticAssetRequest>> {
    @Override
    public Handler<StaticAssetRequest> get() {
      return new DirectoryStaticAssetRequestHandler(
          new HttpCachingStaticAssetRequestHandler(
              new FileStaticAssetRequestHandler()
          )
      );
    }
  }

  public static class StaticAssetsDirProvider implements Provider<File> {
    private final LayoutConfig layoutConfig;
    private final StaticAssetsConfig staticAssetsConfig;

    @Inject
    public StaticAssetsDirProvider(LayoutConfig layoutConfig, StaticAssetsConfig staticAssetsConfig) {
      this.layoutConfig = layoutConfig;
      this.staticAssetsConfig = staticAssetsConfig;
    }

    @Override
    public File get() {
      return new File(layoutConfig.getBaseDir(), staticAssetsConfig.getDir());
    }
  }

  public static class BindingPortProvider implements Provider<Integer> {
    private final DeploymentConfig deploymentConfig;

    @Inject
    public BindingPortProvider(DeploymentConfig deploymentConfig) {
      this.deploymentConfig = deploymentConfig;
    }

    @Override
    public Integer get() {
      return deploymentConfig.getPort();
    }
  }

  public static class BindingHostProvider implements Provider<String> {
    private final DeploymentConfig deploymentConfig;

    @Inject
    public BindingHostProvider(DeploymentConfig deploymentConfig) {
      this.deploymentConfig = deploymentConfig;
    }

    @Override
    public String get() {
      return deploymentConfig.getBindHost();
    }
  }

}
