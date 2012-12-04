package com.bleedingwolf.ratpack

import com.bleedingwolf.ratpack.routing.Router
import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.Context
import org.mortbay.jetty.servlet.ServletHolder
import com.bleedingwolf.ratpack.internal.RatpackServlet

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
    context.setResourceBase(staticFiles.absolutePath)
    def servlet = new RatpackServlet(router, renderer)
    context.addServlet(new ServletHolder(servlet), "/*")
    server.start()
  }

  void stop() {
    server.stop()
    server = null
  }

}
