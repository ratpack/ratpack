package com.bleedingwolf.ratpack

import com.bleedingwolf.ratpack.routing.Router
import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.Context
import org.mortbay.jetty.servlet.ServletHolder
import com.bleedingwolf.ratpack.internal.RatpackServlet
import org.mortbay.jetty.handler.ResourceHandler
import org.mortbay.jetty.handler.HandlerList
import org.mortbay.jetty.servlet.ServletHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.mortbay.resource.Resource

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
    def context = new Context(server, "/", Context.SESSIONS)
    def handlerList = new HandlerList()

    def resourceHandler = new ResourceHandler() {
      @Override
      void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) {
        Resource resource = getResource(request)
        if (resource == null || !resource.equals(getBaseResource())) {
          super.handle(target, request, response, dispatch)
        }
      }
    }
    resourceHandler.resourceBase = staticFiles.absolutePath
    resourceHandler.welcomeFiles = []
    handlerList.addHandler(resourceHandler)

    def servlet = new RatpackServlet(router, renderer)
    def servletHandler = new ServletHandler()
    servletHandler.addServletWithMapping(new ServletHolder(servlet), "/*")
    handlerList.addHandler(servletHandler)

    context.setHandler(handlerList)

    server.start()
  }

  void stop() {
    server.stop()
    server = null
  }

}
