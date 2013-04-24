package org.ratpackframework.http;

import org.ratpackframework.Action;
import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.routing.Routed;

public interface CoreHttpHandlers {

  Action<Routed<HttpExchange>> getAppHandler();

  Action<ErroredHttpExchange> getErrorHandler();

  Action<Routed<HttpExchange>> getNotFoundHandler();

}
