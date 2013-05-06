package org.ratpackframework.http;

import org.ratpackframework.context.Context;

import java.util.Map;

public interface Exchange {

  Request getRequest();

  Response getResponse();

  Context getContext();

  void next();

  void next(Handler... handlers);

  void next(Iterable<Handler> handlers);

  void nextWithContext(Object context, Handler... handlers);

  void nextWithContext(Object context, Iterable<Handler> handlers);

  Map<String, String> getPathTokens();

  Map<String, String> getAllPathTokens();

}
