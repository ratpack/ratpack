package org.ratpackframework.routing

import org.vertx.java.core.buffer.Buffer

class FinalizedResponse {

  final Map<String, Object> headers
  final int status
  final Buffer buffer

  FinalizedResponse(Map<String, Object> headers, int status, Buffer buffer) {
    this.headers = headers
    this.status = status
    this.buffer = buffer
  }

}
