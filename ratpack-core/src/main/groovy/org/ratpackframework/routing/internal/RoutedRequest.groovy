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

package org.ratpackframework.routing.internal

import org.ratpackframework.handler.ErrorHandler
import org.ratpackframework.responder.FinalizedResponse
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.Handler
import org.vertx.java.core.http.HttpServerRequest

class RoutedRequest {

  final HttpServerRequest request
  final ErrorHandler errorHandler
  final Handler<HttpServerRequest> notFoundHandler
  final AsyncResultHandler<FinalizedResponse> finalizedResponseHandler

  RoutedRequest(HttpServerRequest request, ErrorHandler errorHandler, Handler<HttpServerRequest> notFoundHandler, AsyncResultHandler<FinalizedResponse> finalizedResponseHandler) {
    this.request = request
    this.errorHandler = errorHandler
    this.notFoundHandler = notFoundHandler
    this.finalizedResponseHandler = finalizedResponseHandler
  }

  RoutedRequest withNotFoundHandler(Handler<HttpServerRequest> newNotFoundHandler) {
    new RoutedRequest(request, errorHandler, newNotFoundHandler, finalizedResponseHandler)
  }
}
