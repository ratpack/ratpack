/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bleedingwolf.ratpack.request

import com.bleedingwolf.ratpack.TemplateRenderer
import com.bleedingwolf.ratpack.internal.HttpHeader
import com.bleedingwolf.ratpack.internal.MimeType
import groovy.json.JsonBuilder
import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletResponse

@CompileStatic
class Response {

  final TemplateRenderer renderer
  Map<String, Object> headers = [:]
  int status = 200

  final ByteArrayOutputStream output = new ByteArrayOutputStream()

  private final Request request

  Response(Request request, TemplateRenderer renderer) {
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
    contentType = contentType ?: MimeType.TEXT_HTML
    renderString(renderer.render(templateName, context))
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

  /**
   * Sends a temporary redirect response to the client using the specified redirect location URL.
   *
   * @param location the redirect location URL
   */
  void sendRedirect(String location) {
    status = HttpServletResponse.SC_MOVED_TEMPORARILY
    headers[HttpHeader.LOCATION.string] = new URL(new URL(request.servletRequest.requestURL.toString()), location).toString()
  }


}
