package org.ratpackframework.routing;

import org.ratpackframework.Response;

import java.util.Map;

public interface ResponseFactory {
  Response create(RoutedRequest routedRequest, Map<String, String> urlParams);
}
