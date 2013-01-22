package org.ratpackframework.internal

import org.ratpackframework.TemplateRenderer
import org.ratpackframework.Request
import org.ratpackframework.Response
import org.ratpackframework.internal.HttpHeader
import org.ratpackframework.internal.MimeType
import groovy.json.JsonBuilder

import javax.servlet.http.HttpServletResponse

class DefaultResponse implements Response {

  private final TemplateRenderer renderer

  final Map<String, Object> headers = [:]
  int status = 200

  final ByteArrayOutputStream output = new ByteArrayOutputStream()

  private final Request request

  DefaultResponse(Request request, TemplateRenderer renderer) {
    this.request = request
    this.renderer = renderer
  }

  String getContentType() {
    headers[HttpHeader.CONTENT_TYPE.string]
  }

  void setContentType(String contentType) {
    headers[HttpHeader.CONTENT_TYPE.string] = contentType
  }

  String render(Map context = [:], String templateName) {
    contentType = contentType ?: MimeType.TEXT_HTML.string
    renderer.render(templateName, context).writeTo(new OutputStreamWriter(output))
  }

  String renderJson(Object o) {
    contentType = contentType ?: MimeType.APPLICATION_JSON.string
    renderString(new JsonBuilder(o).toString())
  }

  String renderString(String str) {
    contentType = contentType ?: MimeType.TEXT_PLAIN.string
    output << str.bytes
    str
  }

  void sendRedirect(String location) {
    status = HttpServletResponse.SC_MOVED_TEMPORARILY
    headers[HttpHeader.LOCATION.string] = new URL(new URL(request.servletRequest.requestURL.toString()), location).toString()
  }

}
