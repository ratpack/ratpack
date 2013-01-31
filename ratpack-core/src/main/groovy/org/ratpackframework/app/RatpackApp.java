package org.ratpackframework.app;

import org.ratpackframework.assets.*;
import org.ratpackframework.handler.ErrorHandler;
import org.ratpackframework.handler.NotFoundHandler;
import org.ratpackframework.handler.RoutingHandler;
import org.ratpackframework.routing.RoutedRequest;
import org.ratpackframework.templating.TemplateRenderer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class RatpackApp {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Vertx vertx;
  private final HttpServer httpServer;
  private final String host;
  private final int port;
  private final Handler<RoutedRequest> router;
  private final TemplateRenderer templateCompiler;
  private final File staticFiles;

  private final AtomicBoolean running = new AtomicBoolean();
  private final Handler<RatpackApp> onStart;

  private CountDownLatch latch;

  public RatpackApp(Vertx vertx, String host, int port, Handler<RoutedRequest> router, TemplateRenderer templateCompiler, File staticFiles, Handler<RatpackApp> onStart) {
    this.vertx = vertx;
    this.httpServer = vertx.createHttpServer();
    this.host = host;
    this.port = port;
    this.router = router;
    this.templateCompiler = templateCompiler;
    this.staticFiles = staticFiles;
    this.onStart = onStart;
  }

  public void start() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("httpServer already running");
    }

    latch = new CountDownLatch(1);
    ErrorHandler errorHandler = new ErrorHandler(templateCompiler);
    Handler<HttpServerRequest> notFoundHandler = new NotFoundHandler(templateCompiler);

    Handler<StaticAssetRequest> staticHandlerChain = new DirectoryStaticAssetRequestHandler(
        new HttpCachingStaticAssetRequestHandler(
            new FileStaticAssetRequestHandler()
        )
    );

    Handler<HttpServerRequest> staticHandler = new StaticAssetRequestHandlerWrapper(vertx, errorHandler, notFoundHandler, staticFiles.getAbsolutePath(), staticHandlerChain);
    Handler<HttpServerRequest> routingHandler = new RoutingHandler(router, errorHandler, staticHandler);

    httpServer.requestHandler(routingHandler);

    if (onStart != null) {
      onStart.handle(this);
    }

    httpServer.listen(port, host);
    if (logger.isInfoEnabled()) {
      logger.info(String.format("Ratpack started for http://%s:%s", host, port));
    }
  }

  public void startAndWait() {
    start();
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void stop() {
    try {
      httpServer.close();
      running.set(false);
    } finally {
      latch.countDown();
    }
  }

}
