package com.bleedingwolf.ratpack

import com.bleedingwolf.ratpack.internal.RatpackHandler
import com.bleedingwolf.ratpack.routing.Router
import groovy.transform.CompileStatic
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.ResourceHandler

@CompileStatic
class RatpackApp {

  private Server server

  final int port
  final String appPath
  final Router router
  final TemplateRenderer renderer
  final File staticFiles

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

    server = new Server(port)
    def handlerList = new HandlerCollection()

    def resourceHandler = new ResourceHandler()
    resourceHandler.resourceBase = staticFiles.absolutePath
    resourceHandler.welcomeFiles = []

    def handler = new RatpackHandler(router, renderer, resourceHandler)
    handlerList.addHandler(handler)


    server.setHandler(handlerList)

    server.start()
  }

  void stop() {
    server.stop()
    server = null
  }

}
