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

import java.util.concurrent.CountDownLatch

@CompileStatic
class RatpackApp {

  private HttpServer server

  final int port
  final String appPath
  final Router router
  final TemplateRenderer templateCompiler
  final File staticFiles

  private final Vertx vertx

  private CountDownLatch latch

  RatpackApp(Vertx vertx, int port, String appPath, Router router, TemplateRenderer templateCompiler, File staticFiles) {
    this.vertx = vertx
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
    server.listen(port)
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
