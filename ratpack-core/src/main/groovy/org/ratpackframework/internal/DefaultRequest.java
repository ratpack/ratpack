package org.ratpackframework.internal;

import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import org.ratpackframework.Request;
import org.ratpackframework.handler.ClosureHandlerAdapter;
import org.ratpackframework.handler.ErrorHandler;
import org.ratpackframework.handler.ErroredHttpServerRequest;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.Map;

public class DefaultRequest implements Request {

  private ContentType contentType;

  private final HttpServerRequest vertxRequest;
  private final ErrorHandler errorHandler;

  private final Map<String, String> urlParams;
  private Map<String, ?> queryParams;

  public DefaultRequest(final HttpServerRequest vertxRequest, final ErrorHandler errorHandler, Map<String, String> urlParams) {
    this.vertxRequest = vertxRequest;
    this.errorHandler = errorHandler;
    this.urlParams = urlParams;

    vertxRequest.exceptionHandler(new Handler<Exception>() {
      @Override
      public void handle(Exception exception) {
        errorHandler.handle(new ErroredHttpServerRequest(vertxRequest, exception));
      }
    });
  }

  @Override
  public Map<String, ?> getQueryParams() {
    if (queryParams == null) {
      queryParams = new ParamParser().parse(getQuery());
    }
    return queryParams;
  }

  @Override
  public Map<String, String> getUrlParams() {
    return urlParams;
  }

  protected <T> Handler<T> errorHandler(Handler<T> handler) {
    return errorHandler.handler(vertxRequest, handler);
  }

  @Override
  public void buffer(Closure<?> receiver) {
    vertxRequest.bodyHandler(errorHandler(new ClosureHandlerAdapter<Buffer>(receiver)));
  }

  @Override
  public void text(final Closure<?> textReceiver) {
    vertxRequest.bodyHandler(errorHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer event) {
        textReceiver.call(event.toString(getContentType().getCharset()));
      }
    }));
  }

  @Override
  public void json(final Closure<?> jsonReceiver) {
    vertxRequest.bodyHandler(errorHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer event) {
        jsonReceiver.call(new JsonSlurper().parseText(event.toString(getContentType().getCharset())));
      }
    }));
  }

  @Override
  public void form(final Closure<?> formReceiver) {
    vertxRequest.bodyHandler(errorHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer event) {
        formReceiver.call(new ParamParser().parse(event.toString(getContentType().getCharset())));
      }
    }));
  }

  private ContentType getContentType() {
    if (contentType == null) {
      contentType = new ContentType(vertxRequest.headers().get("Content-Type"));
    }
    return contentType;
  }

  @Override
  public String getUri() {
    return vertxRequest.uri;
  }

  @Override
  public String getQuery() {
    return vertxRequest.query;
  }

  @Override
  public String getPath() {
    return vertxRequest.path;
  }
}
