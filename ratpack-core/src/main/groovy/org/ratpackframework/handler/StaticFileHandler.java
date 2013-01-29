package org.ratpackframework.handler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.File;

public class StaticFileHandler implements Handler<HttpServerRequest> {

  private final org.vertx.java.core.file.FileSystem fileSystem;
  private final String contentDirPath;
  private final ErrorHandler errorHandler;
  private final Handler<HttpServerRequest> notFoundHandler;

  public StaticFileHandler(Vertx vertx, File contentDir, ErrorHandler errorHandler, Handler<HttpServerRequest> notFoundHandler) {
    this.fileSystem = vertx.fileSystem();
    this.contentDirPath = contentDir.getAbsolutePath();
    this.errorHandler = errorHandler;
    this.notFoundHandler = notFoundHandler;
  }

  public void handle(final HttpServerRequest request) {
    request.resume();
    if (request.path.equals("/")) {
      request.response.statusCode = 403;
      request.response.end();
      return;
    }

    String relativePath = request.path.substring(1);
    final String filePath = contentDirPath + File.separator + relativePath;

    fileSystem.exists(filePath, errorHandler.asyncHandler(request, new Handler<Boolean>() {
      @Override
      public void handle(Boolean exists) {
        if (!exists) {
          notFoundHandler.handle(request);
          return;
        }

        fileSystem.props(filePath, errorHandler.asyncHandler(request, new Handler<FileProps>() {
          @Override
          public void handle(FileProps props) {
            if (props.isDirectory) {
              request.response.statusCode = 403;
              request.response.end();
            } else {
              request.response.sendFile(filePath);
            }
          }
        }));
      }
    }));
  }

}
