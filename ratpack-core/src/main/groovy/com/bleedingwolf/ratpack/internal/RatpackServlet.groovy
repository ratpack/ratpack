package com.bleedingwolf.ratpack.internal

import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import com.bleedingwolf.ratpack.routing.Router
import com.bleedingwolf.ratpack.request.Response
import org.codehaus.groovy.runtime.StackTraceUtils
import com.bleedingwolf.ratpack.TemplateRenderer

class RatpackServlet extends HttpServlet {

  protected final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass())

  private final Router router
  TemplateRenderer renderer

  RatpackServlet(Router router, TemplateRenderer renderer) {
    this.router = router
    this.renderer = renderer
  }

  void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    def responder = router.route(servletRequest)

    def bytes = new ByteArrayOutputStream()
    def status = 200
    def headers = [:]

    def handler

    if (responder == null) {
      handler = {
        status = new OutputStreamWriter(bytes).withWriter { notFound(servletRequest, it) }
      }
    } else {
      try {
        def response = new Response(renderer)
        responder.respond(response)
        handler = {
          headers = response.headers
          status = response.status
          bytes << response.output
        }
      } catch (Exception e) {
        handler = {
          status = new OutputStreamWriter(bytes).withWriter { error(servletRequest, e, it) }
        }
      }
    }

    handler()

    servletResponse.status = status
    headers.each { k, v ->
      (v instanceof Iterable ? v.toList() : [v]).each {
        servletResponse.addHeader(k.toString(), it.toString())
      }
    }
    servletResponse.setContentLength(bytes.size())
    servletResponse.outputStream.withStream {
      it << bytes
    }

    logger.info("[   ${status}] ${servletRequest.method} ${servletRequest.pathInfo}")
  }

  int error(HttpServletRequest servletRequest, Exception error, Writer writer) {
    StackTraceUtils.deepSanitize(error)
    error.printStackTrace()
    writer << renderer.renderException(error, servletRequest)
    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
  }

  int notFound(HttpServletRequest servletRequest, Writer writer) {
    writer << renderer.renderError(
        title: 'Page Not Found',
        message: 'Page Not Found',
        metadata: [
            'Request Method': servletRequest.method.toUpperCase(),
            'Request URL': servletRequest.requestURL,
        ]
    )
    HttpServletResponse.SC_NOT_FOUND
  }

}
