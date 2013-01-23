package org.ratpackframework.app

import groovy.transform.CompileStatic
import org.ratpackframework.handler.*
import org.ratpackframework.routing.Router
import org.ratpackframework.templating.TemplateRenderer
import org.vertx.java.core.Vertx
import org.vertx.java.core.http.HttpServer
import org.vertx.java.core.http.HttpServerRequest

@CompileStatic
class RatpackApp {

  private HttpServer server

  final int port
  final String appPath
  final Router router
  final TemplateRenderer renderer
  final File staticFiles

  private final Vertx vertx = Vertx.newVertx()

  RatpackApp(int port, String appPath, Router router, TemplateRenderer renderer, File staticFiles) {
    this.port = port
    this.appPath = appPath
    this.router = router
    this.renderer = renderer
    this.staticFiles = staticFiles
  }

  void start() {
    if (server != null) {
      throw new IllegalStateException("server already running")
    }

    server = vertx.createHttpServer()
    server.setAcceptBacklog(10000)
    List<MaybeHandler<HttpServerRequest>> handlers = new ArrayList<>()
    handlers.add(new RoutingHandler(router, renderer))
    handlers.add(new StaticFileHandler(staticFiles))
    handlers.add(new NotFoundHandler(renderer))
    server.requestHandler(new CompositeHandler(renderer, handlers))
    server.listen(port)
  }

  void stop() {
    server.close()
    server = null
  }

}
