package org.ratpackframework.routing;

import org.ratpackframework.context.Context;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;

import java.util.Map;

public interface Exchange {

  Request getRequest();

  Response getResponse();

  Context getContext();

  <T> T get(Class<T> type);

  <T> T maybeGet(Class<T> type);

  void next();

  void next(Handler... handlers);

  void next(Iterable<Handler> handlers);

  void nextWithContext(Object object, Handler... handlers);

  void nextWithContext(Object object, Iterable<Handler> handlers);

  void nextWithContext(Context context, Handler... handlers);

  void nextWithContext(Context context, Iterable<Handler> handlers);

  Map<String, String> getPathTokens();

  Map<String, String> getAllPathTokens();

  void error(Exception exception);

}
