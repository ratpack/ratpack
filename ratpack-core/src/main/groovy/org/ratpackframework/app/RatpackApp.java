package org.ratpackframework.app;

import org.ratpackframework.assets.*;
import org.ratpackframework.handler.ErrorHandler;
import org.ratpackframework.handler.NotFoundHandler;
import org.ratpackframework.handler.RoutingHandler;
import org.ratpackframework.routing.Router;
import org.ratpackframework.session.internal.SessionManager;
import org.ratpackframework.templating.TemplateRenderer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class RatpackApp {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private HttpServer server;

  final Vertx vertx;
  final String host;
  final int port;
  final Router router;
  final TemplateRenderer templateCompiler;
  final File staticFiles;
  private SessionManager sessionManager;

  private CountDownLatch latch;

  public RatpackApp(Vertx vertx, String host, int port, Router router, TemplateRenderer templateCompiler, File staticFiles, SessionManager sessionManager) {
    this.vertx = vertx;
    this.host = host;
    this.port = port;
    this.router = router;
    this.templateCompiler = templateCompiler;
    this.staticFiles = staticFiles;
    this.sessionManager = sessionManager;
  }

  public void start() {
    if (server != null) {
      throw new IllegalStateException("server already running");
    }

    server = vertx.createHttpServer();
    latch = new CountDownLatch(1);
    ErrorHandler errorHandler = new ErrorHandler(templateCompiler);
    Handler<HttpServerRequest> notFoundHandler = new NotFoundHandler(templateCompiler);

    Handler<StaticAssetRequest> staticHandlerChain = new DirectoryStaticAssetRequestHandler(
        new HttpCachingStaticAssetRequestHandler(
            new FileStaticAssetRequestHandler()
        )
    );

    Handler<HttpServerRequest> staticHandler = new StaticAssetRequestHandlerWrapper(vertx, errorHandler, notFoundHandler, staticFiles.getAbsolutePath(), staticHandlerChain);
    Handler<HttpServerRequest> routingHandler = new RoutingHandler(router, errorHandler, staticHandler, sessionManager);

    server.requestHandler(routingHandler);
    server.listen(port, host);
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

  void stop() {
    try {
      server.close();
    } finally {
      latch.countDown();
      server = null;
    }
  }

}
