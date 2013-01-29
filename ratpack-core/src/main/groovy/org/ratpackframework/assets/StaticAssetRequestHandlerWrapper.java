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

package org.ratpackframework.assets;

import org.ratpackframework.handler.ErrorHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;

public class StaticAssetRequestHandlerWrapper implements Handler<HttpServerRequest> {

  private final Vertx vertx;
  private final ErrorHandler errorHandler;
  private final Handler<HttpServerRequest> notFoundHandler;
  private final String assetsDirPath;
  private final Handler<StaticAssetRequest> handler;

  public StaticAssetRequestHandlerWrapper(Vertx vertx, ErrorHandler errorHandler, Handler<HttpServerRequest> notFoundHandler, String assetsDirPath, Handler<StaticAssetRequest> handler) {
    this.vertx = vertx;
    this.errorHandler = errorHandler;
    this.notFoundHandler = notFoundHandler;
    this.assetsDirPath = assetsDirPath;
    this.handler = handler;
  }

  @Override
  public void handle(HttpServerRequest request) {

    // We are assuming we are at the end of the chain and don't care about the body, so resume.
    request.resume();

    if (request.path.equals("/")) {
      request.response.statusCode = 403;
      request.response.end();
      return;
    }

    handler.handle(new DefaultStaticAssetRequest(vertx, request, errorHandler, notFoundHandler, assetsDirPath));
  }
}
