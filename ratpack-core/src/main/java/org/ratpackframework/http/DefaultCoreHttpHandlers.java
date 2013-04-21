package org.ratpackframework.http;

import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.routing.Routed;

public class DefaultCoreHttpHandlers implements CoreHttpHandlers {

  private final Handler<Routed<HttpExchange>> appHandler;
  private final Handler<ErroredHttpExchange> errorHandler;
  private final Handler<Routed<HttpExchange>> notFoundHandler;

  public DefaultCoreHttpHandlers(Handler<Routed<HttpExchange>> appHandler, Handler<ErroredHttpExchange> errorHandler, Handler<Routed<HttpExchange>> notFoundHandler) {
    this.appHandler = appHandler;
    this.errorHandler = errorHandler;
    this.notFoundHandler = notFoundHandler;
  }

  public Handler<Routed<HttpExchange>> getAppHandler() {
    return appHandler;
  }

  public Handler<ErroredHttpExchange> getErrorHandler() {
    return errorHandler;
  }

  public Handler<Routed<HttpExchange>> getNotFoundHandler() {
    return notFoundHandler;
  }
}
