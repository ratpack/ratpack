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

package org.ratpackframework.handler

import org.ratpackframework.templating.CompiledTemplate
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.buffer.Buffer
import org.vertx.java.core.http.HttpServerRequest

class FallbackErrorHandlingTemplateRenderer implements AsyncResultHandler<CompiledTemplate> {

  private final HttpServerRequest request
  private final String operationDescription

  FallbackErrorHandlingTemplateRenderer(HttpServerRequest request, String operationDescription) {
    this.request = request
    this.operationDescription = operationDescription
  }

  @Override
  void handle(AsyncResult<CompiledTemplate> compileEvent) {
    final response = request.response

    if (compileEvent.failed()) {
      compileEvent.exception.printStackTrace(System.err)
      response.statusMessage = "Unhandled exception occurred during $operationDescription"
      response.statusCode = 500
      response.end()
    } else {
      compileEvent.result.render(new AsyncResultHandler<Buffer>() {
        @Override
        void handle(AsyncResult<Buffer> renderEvent) {
          if (renderEvent.failed()) {
            renderEvent.exception.printStackTrace(System.err)
            response.statusMessage = "Unhandled exception occurred during $operationDescription"
            response.statusCode = 500
            response.end()
          } else {
            response.end(renderEvent.result)
          }
        }
      })

    }
  }
}
