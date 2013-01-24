package org.ratpackframework.app

import groovy.transform.CompileStatic
import org.ratpackframework.handler.*
import org.ratpackframework.routing.Router
import org.ratpackframework.templating.TemplateCompiler
import org.vertx.java.core.Handler
import org.vertx.java.core.Vertx
import org.vertx.java.core.http.HttpServer
import org.vertx.java.core.http.HttpServerRequest

@CompileStatic
class RatpackApp {

  private HttpServer server

  final int port
  final String appPath
  final Router router
  final TemplateCompiler templateCompiler
  final File staticFiles

  private final Vertx vertx

  RatpackApp(Vertx vertx, int port, String appPath, Router router, TemplateCompiler templateCompiler, File staticFiles) {
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
    ErrorHandler errorHandler = new ErrorHandler(templateCompiler)
    def notFoundHandler = new NotFoundHandler(templateCompiler)

    def staticHandler = new StaticFileHandler(vertx, staticFiles, errorHandler, notFoundHandler)
    def routingHandler = new RoutingHandler(router, errorHandler, staticHandler)

    server.requestHandler(routingHandler)
    server.listen(port)
  }

  void stop() {
    server.close()
    server = null
  }

}
