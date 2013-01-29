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
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.file.FileSystemProps;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.File;

public class DefaultStaticAssetRequest implements StaticAssetRequest {

  private final Vertx vertx;
  private final HttpServerRequest request;
  private final ErrorHandler errorHandler;
  private final Handler<HttpServerRequest> notFoundHandler;
  private final String filePath;

  private Boolean exists;
  private FileProps props;

  public DefaultStaticAssetRequest(Vertx vertx, HttpServerRequest request, ErrorHandler errorHandler, Handler<HttpServerRequest> notFoundHandler, String assetsDirPath) {
    this.vertx = vertx;
    this.request = request;
    this.errorHandler = errorHandler;
    this.notFoundHandler = notFoundHandler;
    this.filePath = assetsDirPath + File.separator + request.path.substring(1);
  }

  public String getFilePath() {
    return filePath;
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

  @Override
  public void props(final Handler<FileProps> handler) {
    if (props != null) {
      handler.handle(props);
    } else {
      vertx.fileSystem().props(filePath, errorHandler.asyncHandler(request, new Handler<FileProps>() {
        @Override
        public void handle(FileProps result) {
          props = result;
          handler.handle(props);
        }
      }));
    }
  }

  @Override
  public void exists(final Handler<Boolean> handler) {
    if (exists != null) {
      handler.handle(exists);
    } else {
      vertx.fileSystem().exists(filePath, errorHandler.asyncHandler(request, new Handler<Boolean>() {
        @Override
        public void handle(Boolean result) {
          exists = result;
          handler.handle(exists);
        }
      }));
    }
  }
}
