package org.ratpackframework.app

import groovy.transform.CompileStatic
import org.ratpackframework.assets.*
import org.ratpackframework.handler.ErrorHandler
import org.ratpackframework.handler.NotFoundHandler
import org.ratpackframework.handler.RoutingHandler
import org.ratpackframework.routing.Router
import org.ratpackframework.templating.TemplateRenderer
import org.vertx.java.core.Handler
import org.vertx.java.core.Vertx
import org.vertx.java.core.http.HttpServer
import org.vertx.java.core.logging.Logger
import org.vertx.java.core.logging.impl.LoggerFactory

import java.util.concurrent.CountDownLatch

@CompileStatic
class RatpackApp {

  private final Logger logger = LoggerFactory.getLogger(getClass())

  private HttpServer server

  final Vertx vertx
  final String host
  final int port
  final String appPath
  final Router router
  final TemplateRenderer templateCompiler
  final File staticFiles

  private CountDownLatch latch

  RatpackApp(Vertx vertx, String host, int port, Router router, TemplateRenderer templateCompiler, File staticFiles) {
    this.vertx = vertx
    this.host = host
    this.port = port
    this.appPath = appPath
    this.router = router
    this.templateCompiler = templateCompiler
    this.staticFiles = staticFiles
  }

  void start() {
    if (server != null) {
      throw new IllegalStateException("server already running")
    }

    server = vertx.createHttpServer()
    latch = new CountDownLatch(1)
    ErrorHandler errorHandler = new ErrorHandler(templateCompiler)
    def notFoundHandler = new NotFoundHandler(templateCompiler)

    def Handler<StaticAssetRequest> staticHandlerChain = new DirectoryStaticAssetRequestHandler(
        new HttpCachingStaticAssetRequestHandler(
            new FileStaticAssetRequestHandler()
        )
    )

    def staticHandler = new StaticAssetRequestHandlerWrapper(vertx, errorHandler, notFoundHandler, staticFiles.absolutePath, staticHandlerChain)
    def routingHandler = new RoutingHandler(router, errorHandler, staticHandler)

    server.requestHandler(routingHandler)
    server.listen(port, host)
    if (logger.isInfoEnabled()) {
      logger.info(String.format("Ratpack started for http://%s:%s", host, port))
    }
  }

  void startAndWait() {
    start()
    latch.await()
  }

  void stop() {
    server.close()
    server = null
    latch.countDown()
  }

}
