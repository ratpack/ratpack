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

import com.bleedingwolf.ratpack.internal.MimeTypes
import com.bleedingwolf.ratpack.TemplateRenderer
import groovy.json.JsonBuilder

import org.mortbay.jetty.HttpHeaders

class Response {

  final TemplateRenderer renderer
  Map<String, ?> headers = [:]
  int status = 200

  final ByteArrayOutputStream output = new ByteArrayOutputStream()

  Response(TemplateRenderer renderer) {
    this.renderer = renderer
  }

  String getContentType() {
    headers[HttpHeaders.CONTENT_TYPE]
  }

  void setContentType(String contentType) {
    headers[HttpHeaders.CONTENT_TYPE] = contentType
  }

  String render(Map context = [:], def templateName) {
    contentType = contentType ?: MimeTypes.TEXT_HTML
    renderString(renderer.render(templateName, context))
  }

  String renderJson(Object o) {
    contentType = contentType ?: MimeTypes.APPLICATION_JSON
    renderString(new JsonBuilder(o).toString())
  }

  String renderString(String str) {
    contentType = contentType ?: MimeTypes.TEXT_PLAIN
    output << str.bytes
    str
  }

}
