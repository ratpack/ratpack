package org.ratpackframework.bootstrap.internal;

import org.ratpackframework.assets.StaticAssetRequest;
import org.ratpackframework.assets.StaticAssetRequestHandlerWrapper;
import org.ratpackframework.bootstrap.RatpackApp;
import org.ratpackframework.handler.ErrorHandler;
import org.ratpackframework.handler.NotFoundHandler;
import org.ratpackframework.handler.RoutingHandler;
import org.ratpackframework.routing.RoutedRequest;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultRatpackApp implements RatpackApp {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Vertx vertx;
  private final HttpServer httpServer;
  private final String bindHost;
  private final int bindPort;
  private final Handler<RoutedRequest> routedRequestHandler;
  private final Handler<StaticAssetRequest> staticAssetRequestHandler;
  private final File staticAssetDir;
  private final ErrorHandler errorHandler;
  private final NotFoundHandler notFoundHandler;
  private final Handler<RatpackApp> init;

  private final AtomicBoolean running = new AtomicBoolean();
  private CountDownLatch latch;

  @Inject
  public DefaultRatpackApp(
      Vertx vertx, HttpServer httpServer,
      @Named("bindHost") String bindHost, @Named("bindPort") int bindPort,
      Handler<RoutedRequest> routedRequestHandler,
      Handler<StaticAssetRequest> staticAssetRequestHandler, @Named("staticAssetDir") File staticAssetDir,
      ErrorHandler errorHandler, NotFoundHandler notFoundHandler,
      Handler<RatpackApp> init
  ) {
    this.vertx = vertx;
    this.httpServer = httpServer;
    this.bindHost = bindHost;
    this.bindPort = bindPort;
    this.routedRequestHandler = routedRequestHandler;
    this.staticAssetRequestHandler = staticAssetRequestHandler;
    this.staticAssetDir = staticAssetDir;
    this.errorHandler = errorHandler;
    this.notFoundHandler = notFoundHandler;
    this.init = init;
  }

  @Override
  public void start() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("httpServer already running");
    }

    latch = new CountDownLatch(1);

    Handler<HttpServerRequest> staticHandler = new StaticAssetRequestHandlerWrapper(vertx, errorHandler, notFoundHandler, staticAssetDir.getAbsolutePath(), staticAssetRequestHandler);
    Handler<HttpServerRequest> routingHandler = new RoutingHandler(routedRequestHandler, errorHandler, staticHandler);

    httpServer.requestHandler(routingHandler);

    init.handle(this);

    httpServer.listen(bindPort, bindHost);
    if (logger.isInfoEnabled()) {
      logger.info(String.format("Ratpack started for http://%s:%s", bindHost, bindPort));
    }
  }

  @Override
  public void startAndWait() {
    start();
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stop() {
    try {
      httpServer.close();
      running.set(false);
    } finally {
      latch.countDown();
    }
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public int getBindPort() {
    return bindPort;
  }

  @Override
  public String getBindHost() {
    return bindHost;
  }

}
