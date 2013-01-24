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

package org.ratpackframework.responder.internal

import org.ratpackframework.templating.TemplateCompiler
import org.ratpackframework.Request
import org.ratpackframework.Response
import org.ratpackframework.internal.DefaultResponse
import org.ratpackframework.responder.FinalizedResponse
import org.ratpackframework.responder.Responder
import groovy.transform.CompileStatic
import org.vertx.java.core.AsyncResultHandler

@CompileStatic
abstract class AbstractResponder implements Responder {

  private final Request request
  private final TemplateCompiler templateCompiler

  AbstractResponder(Request request, TemplateCompiler templateCompiler) {
    this.request = request
    this.templateCompiler = templateCompiler
  }

  @Override
  void respond(AsyncResultHandler<FinalizedResponse> handler) {
    def response = new DefaultResponse(templateCompiler, handler)
    try {
      doRespond(request, response)
    } catch (Exception e) {
      response.error(e)
    }
  }

  abstract void doRespond(Request request, Response response)
}
