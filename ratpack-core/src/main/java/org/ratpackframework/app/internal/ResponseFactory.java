package org.ratpackframework.app.internal;

import org.ratpackframework.app.Response;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import java.util.Map;

public interface ResponseFactory {
  Response create(Routed<HttpExchange> routedHttpExchange, Map<String, String> urlParams);
}
