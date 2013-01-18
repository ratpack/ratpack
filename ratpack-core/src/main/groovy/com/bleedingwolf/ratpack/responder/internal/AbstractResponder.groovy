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

package com.bleedingwolf.ratpack.responder.internal

import com.bleedingwolf.ratpack.TemplateRenderer
import com.bleedingwolf.ratpack.responder.FinalizedResponse
import com.bleedingwolf.ratpack.responder.Responder
import com.bleedingwolf.ratpack.handler.Request
import com.bleedingwolf.ratpack.handler.Response
import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractResponder implements Responder {

  private final Request request
  private final TemplateRenderer templateRenderer

  AbstractResponder(Request request, TemplateRenderer templateRenderer) {
    this.request = request
    this.templateRenderer = templateRenderer
  }

  @Override
  FinalizedResponse respond() {
    def response = new Response(request, templateRenderer)
    doRespond(request, response)
    new FinalizedResponse(response.headers, response.status, response.output.toByteArray())
  }

  abstract void doRespond(Request request, Response response)
}
