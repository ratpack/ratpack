package com.bleedingwolf.ratpack.handler.internal

import com.bleedingwolf.ratpack.handler.Request
import com.bleedingwolf.ratpack.internal.ClosureHandlerAdapter
import com.bleedingwolf.ratpack.internal.ParamParser
import groovy.json.JsonSlurper
import org.vertx.java.core.Handler
import org.vertx.java.core.buffer.Buffer
import org.vertx.java.core.http.HttpServerRequest

class DefaultRequest implements Request {

  private ContentType contentType

  private final HttpServerRequest vertxRequest

  final Map<String, String> urlParams
  @Lazy Map<String, ?> queryParams = new ParamParser().parse(vertxRequest.query)

  DefaultRequest(HttpServerRequest vertxRequest, Map<String, String> urlParams) {
    this.vertxRequest = vertxRequest
    this.urlParams = urlParams
  }

  @Override
  void buffer(Closure<?> receiver) {
    vertxRequest.bodyHandler(new ClosureHandlerAdapter<Buffer>(receiver))
  }

  @Override
  void text(Closure<?> textReceiver) {
    vertxRequest.bodyHandler(new Handler<Buffer>() {
      @Override
      void handle(Buffer event) {
        textReceiver(event.toString(getContentType().charset))
      }
    })
  }

  @Override
  void json(Closure<?> jsonReceiver) {
    vertxRequest.bodyHandler(new Handler<Buffer>() {
      @Override
      void handle(Buffer event) {
        jsonReceiver(new JsonSlurper().parseText(event.toString(getContentType().charset)))
      }
    })
  }

  @Override
  void form(Closure<?> formReceiver) {
    vertxRequest.bodyHandler(new Handler<Buffer>() {
      @Override
      void handle(Buffer event) {
        formReceiver(new ParamParser().parse(event.toString(getContentType().charset)))
      }
    })
  }

  private ContentType getContentType() {
    if (contentType == null) {
      contentType = new ContentType(vertxRequest.headers().get("Content-Type"))
    }
    contentType
  }

  @Override
  String getUri() {
    vertxRequest.uri
  }

  @Override
  String getQuery() {
    vertxRequest.query
  }

  @Override
  String getPath() {
    vertxRequest.path
  }
}
