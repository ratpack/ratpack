package org.ratpackframework.handler

import groovy.transform.CompileStatic
import org.vertx.java.core.http.HttpServerRequest

@CompileStatic
class StaticFileHandler implements MaybeHandler<HttpServerRequest> {

  private final File content

  StaticFileHandler(File content) {
    this.content = content
  }

  boolean maybeHandle(HttpServerRequest request) {
    if (request.path == "/") {
      request.response.statusCode = 403
      request.response.end()
      return true
    }

    String relativePath = request.path[1..-1]
    File file = new File(content, relativePath)

    if (file.isFile()) {
      request.response.sendFile(file.absolutePath)
      true
    } else if (file.isDirectory()) {
      request.response.statusCode = 403
      request.response.end()
      true
    } else {
      false
    }
  }


}
