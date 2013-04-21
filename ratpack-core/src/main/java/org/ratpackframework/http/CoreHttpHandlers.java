package org.ratpackframework.http;

import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.routing.Routed;

public interface CoreHttpHandlers {

  Handler<Routed<HttpExchange>> getAppHandler();

  Handler<ErroredHttpExchange> getErrorHandler();

  Handler<Routed<HttpExchange>> getNotFoundHandler();

}
