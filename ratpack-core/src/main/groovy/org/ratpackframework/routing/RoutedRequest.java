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

package org.ratpackframework.routing;

import org.ratpackframework.handler.ErrorHandler;
import org.ratpackframework.routing.FinalizedResponse;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class RoutedRequest {

  private final HttpServerRequest request;
  private final ErrorHandler errorHandler;
  private final Handler<HttpServerRequest> notFoundHandler;
  private final AsyncResultHandler<FinalizedResponse> finalizedResponseHandler;

  public RoutedRequest(HttpServerRequest request, ErrorHandler errorHandler, Handler<HttpServerRequest> notFoundHandler, AsyncResultHandler<FinalizedResponse> finalizedResponseHandler) {
    this.request = request;
    this.errorHandler = errorHandler;
    this.notFoundHandler = notFoundHandler;
    this.finalizedResponseHandler = finalizedResponseHandler;
  }

  public RoutedRequest withNotFoundHandler(Handler<HttpServerRequest> newNotFoundHandler) {
    return new RoutedRequest(request, errorHandler, newNotFoundHandler, finalizedResponseHandler);
  }

  public HttpServerRequest getRequest() {
    return request;
  }

  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public Handler<HttpServerRequest> getNotFoundHandler() {
    return notFoundHandler;
  }

  public AsyncResultHandler<FinalizedResponse> getFinalizedResponseHandler() {
    return finalizedResponseHandler;
  }

}
