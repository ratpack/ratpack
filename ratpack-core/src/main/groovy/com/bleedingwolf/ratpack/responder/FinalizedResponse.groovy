package com.bleedingwolf.ratpack.responder

class FinalizedResponse {

  final Map<String, Object> headers
  final int status
  final byte[] bytes

  FinalizedResponse(Map<String, Object> headers, int status, byte[] bytes) {
    this.headers = headers
    this.status = status
    this.bytes = bytes
  }

}
