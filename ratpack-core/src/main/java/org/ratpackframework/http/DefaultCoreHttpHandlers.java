package org.ratpackframework.http;

import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.Action;
import org.ratpackframework.routing.Routed;

public class DefaultCoreHttpHandlers implements CoreHttpHandlers {

  private final Action<Routed<HttpExchange>> appHandler;
  private final Action<ErroredHttpExchange> errorHandler;
  private final Action<Routed<HttpExchange>> notFoundHandler;

  public DefaultCoreHttpHandlers(Action<Routed<HttpExchange>> appHandler, Action<ErroredHttpExchange> errorHandler, Action<Routed<HttpExchange>> notFoundHandler) {
    this.appHandler = appHandler;
    this.errorHandler = errorHandler;
    this.notFoundHandler = notFoundHandler;
  }

  public Action<Routed<HttpExchange>> getAppHandler() {
    return appHandler;
  }

  public Action<ErroredHttpExchange> getErrorHandler() {
    return errorHandler;
  }

  public Action<Routed<HttpExchange>> getNotFoundHandler() {
    return notFoundHandler;
  }
}
