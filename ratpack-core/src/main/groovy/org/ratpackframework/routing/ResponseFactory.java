package org.ratpackframework.routing;

import org.ratpackframework.Response;
import org.ratpackframework.handler.HttpExchange;

import java.util.Map;

public interface ResponseFactory {
  Response create(Routed<HttpExchange> routedHttpExchange, Map<String, String> urlParams);
}
