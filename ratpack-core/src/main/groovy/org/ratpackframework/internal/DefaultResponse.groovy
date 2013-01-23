package org.ratpackframework.internal

import groovy.json.JsonBuilder
import org.ratpackframework.Response
import org.ratpackframework.templating.TemplateRenderer

class DefaultResponse implements Response {

  private final TemplateRenderer renderer

  final Map<String, Object> headers = [:]
  int status = 200

  final ByteArrayOutputStream output = new ByteArrayOutputStream()

  private final String requestUri

  DefaultResponse(String requestUri, TemplateRenderer renderer) {
    this.requestUri = requestUri
    this.renderer = renderer
  }

  String getContentType() {
    headers[HttpHeader.CONTENT_TYPE]
  }

  void setContentType(String contentType) {
    headers[HttpHeader.CONTENT_TYPE] = contentType
  }

  String render(Map context = [:], String templateName) {
    contentType = contentType ?: MimeType.TEXT_HTML
    renderer.render(templateName, context).writeTo(new OutputStreamWriter(output))
  }

  String renderJson(Object o) {
    contentType = contentType ?: MimeType.APPLICATION_JSON
    renderString(new JsonBuilder(o).toString())
  }

  String renderString(String str) {
    contentType = contentType ?: MimeType.TEXT_PLAIN
    output << str.bytes
    str
  }

  void sendRedirect(String location) {
    status = 301
    headers[HttpHeader.LOCATION] = location
  }

}
