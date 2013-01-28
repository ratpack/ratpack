package org.ratpackframework.internal

import groovy.json.JsonSlurper
import org.ratpackframework.Request
import org.ratpackframework.handler.ClosureHandlerAdapter
import org.ratpackframework.handler.ErrorHandler
import org.ratpackframework.handler.ErroredHttpServerRequest
import org.vertx.java.core.Handler
import org.vertx.java.core.buffer.Buffer
import org.vertx.java.core.http.HttpServerRequest

class DefaultRequest implements Request {

  private ContentType contentType

  private final HttpServerRequest vertxRequest
  private final ErrorHandler errorHandler

  final Map<String, String> urlParams
  @Lazy Map<String, ?> queryParams = new ParamParser().parse(vertxRequest.query)

  DefaultRequest(HttpServerRequest vertxRequest, ErrorHandler errorHandler, Map<String, String> urlParams) {
    this.vertxRequest = vertxRequest
    this.errorHandler = errorHandler
    this.urlParams = urlParams

    vertxRequest.exceptionHandler(new Handler<Exception>() {
      @Override
      void handle(Exception exception) {
        errorHandler.handle(new ErroredHttpServerRequest(vertxRequest, exception))
      }
    })
  }

  protected <T> Handler<T> errorHandler(Handler<T> handler) {
    return errorHandler.handler(vertxRequest, handler);
  }

  @Override
  void buffer(Closure<?> receiver) {
    vertxRequest.bodyHandler(errorHandler(new ClosureHandlerAdapter<Buffer>(receiver)))
  }

  @Override
  void text(Closure<?> textReceiver) {
    vertxRequest.bodyHandler(errorHandler(new Handler<Buffer>() {
      @Override
      void handle(Buffer event) {
        textReceiver(event.toString(getContentType().charset))
      }
    }))
  }

  @Override
  void json(Closure<?> jsonReceiver) {
    vertxRequest.bodyHandler(errorHandler(new Handler<Buffer>() {
      @Override
      void handle(Buffer event) {
        errorHandler.wrap(vertxRequest) {
          jsonReceiver(new JsonSlurper().parseText(event.toString(getContentType().charset)))
        }
      }
    }))
  }

  @Override
  void form(Closure<?> formReceiver) {
    vertxRequest.bodyHandler(errorHandler(new Handler<Buffer>() {
      @Override
      void handle(Buffer event) {

        formReceiver(new ParamParser().parse(event.toString(getContentType().charset)))
      }
    }))
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
